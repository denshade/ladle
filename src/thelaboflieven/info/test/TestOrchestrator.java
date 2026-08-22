package thelaboflieven.info.test;

import thelaboflieven.info.CommandLine;
import thelaboflieven.info.CommandsRunner;
import thelaboflieven.info.ProjectContext;
import thelaboflieven.info.build.Subproject;
import thelaboflieven.info.build.Subprojects;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class TestOrchestrator {
    private final Function<File, CommandsRunner> runnerFactory;

    public TestOrchestrator() {
        this(CommandsRunner::new);
    }

    TestOrchestrator(Function<File, CommandsRunner> runnerFactory) {
        this.runnerFactory = runnerFactory;
    }

    public int test(File iniFile) throws IOException, InterruptedException {
        return test(ProjectContext.load(iniFile.getAbsolutePath()), new HashSet<>(), true);
    }

    private int test(
            ProjectContext project,
            Set<String> visitedInChain,
            boolean isRoot
    ) throws IOException, InterruptedException {
        var canonicalPath = project.iniFile().getCanonicalPath();
        if (!visitedInChain.add(canonicalPath)) {
            throw new IllegalStateException("Circular subproject reference: " + project.iniFile().getPath());
        }

        try {
            int testClassCount = 0;
            var subprojects = Subprojects.read(project.iniData());
            for (var subproject : subprojects) {
                testClassCount += testSubproject(project.projectDir(), subproject, visitedInChain);
            }

            if (project.iniData().get("test") != null) {
                testClassCount += runProjectTests(project);
            } else if (subprojects.isEmpty()) {
                if (isRoot) {
                    throw new IllegalStateException(
                            "Missing [test] section in INI file. Omit it only when [subproject] is present.");
                }
                System.out.println("No [test] in " + project.iniFile().getName() + "; skipping.");
            } else {
                System.out.println(
                        "No [test] in " + project.iniFile().getName() + "; testing subprojects only.");
            }
            return testClassCount;
        } finally {
            visitedInChain.remove(canonicalPath);
        }
    }

    private int testSubproject(
            File projectDir,
            Subproject subproject,
            Set<String> visitedInChain
    ) throws IOException, InterruptedException {
        System.out.println("Testing subproject " + subproject.name() + " (" + subproject.path() + ")");
        return test(Subprojects.load(projectDir, subproject), visitedInChain, false);
    }

    private int runProjectTests(ProjectContext project) throws IOException, InterruptedException {
        var plan = new TestCommandBuilder(project).buildPlan();
        if (plan.testClassCount() == 0) {
            System.err.println("Warning: no test classes found in " + project.iniFile().getName() + ".");
            return 0;
        }
        printTestPlan(project.iniFile(), plan);
        var exitCode = runnerFactory.apply(project.projectDir()).run(plan.commands());
        if (exitCode != 0) {
            throw new TestFailedException(exitCode);
        }
        return plan.testClassCount();
    }

    private static void printTestPlan(File buildIni, TestPlan plan) {
        System.out.println("Testing from " + buildIni.getName());
        System.out.println("Running " + plan.testClassCount() + " test class(es) with " + frameworkLabel(plan.runner()));
        System.out.println("  java: " + plan.javaPath());
        System.out.println("  classpath: " + plan.classpath());
        System.out.println("  runner: " + plan.runner());
        for (int i = 0; i < plan.commands().size(); i++) {
            System.out.println("  command " + (i + 1) + ": " + CommandLine.format(plan.commands().get(i)));
        }
    }

    private static String frameworkLabel(String runner) {
        if (TestCommandBuilder.JUNIT4_RUNNER.equals(runner)) {
            return "JUnit 4";
        }
        return "JUnit 5";
    }
}
