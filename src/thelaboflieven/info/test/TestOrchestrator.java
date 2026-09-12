package thelaboflieven.info.test;

import thelaboflieven.info.CommandFailedException;
import thelaboflieven.info.CommandLine;
import thelaboflieven.info.CommandsRunner;
import thelaboflieven.info.ProjectContext;
import thelaboflieven.info.build.BuildConfig;
import thelaboflieven.info.build.CompileOrchestrator;
import thelaboflieven.info.build.Subprojects;
import thelaboflieven.info.download.DependencyOrchestrator;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class TestOrchestrator {
    private final Function<File, CommandsRunner> runnerFactory;
    private final CompileOrchestrator compileOrchestrator = new CompileOrchestrator();
    private final DependencyOrchestrator dependencyOrchestrator = new DependencyOrchestrator();

    public TestOrchestrator() {
        this(CommandsRunner::new);
    }

    TestOrchestrator(Function<File, CommandsRunner> runnerFactory) {
        this.runnerFactory = runnerFactory;
    }

    public int test(File iniFile) throws IOException, InterruptedException {
        return test(iniFile, List.of());
    }

    public int test(File iniFile, List<String> classFilters) throws IOException, InterruptedException {
        return test(
                ProjectContext.load(iniFile.getAbsolutePath()),
                new HashSet<>(),
                true,
                classFilters == null ? List.of() : classFilters);
    }

    private int test(
            ProjectContext project,
            Set<String> visitedInChain,
            boolean isRoot,
            List<String> classFilters
    ) throws IOException, InterruptedException {
        return Subprojects.withCycleGuard(project, visitedInChain, () -> {
            var subprojects = Subprojects.read(project.iniData());
            if (isRoot) {
                if (project.iniData().get("test") == null && subprojects.isEmpty()) {
                    throw new IllegalStateException(
                            "Missing [test] section in INI file. Omit it only when [subproject] is present.");
                }
                if (BuildConfig.hasSources(project.iniData()) || !subprojects.isEmpty()) {
                    compileOrchestrator.compile(project);
                } else {
                    dependencyOrchestrator.install(project);
                }
            }

            int testClassCount = 0;
            for (var subproject : subprojects) {
                System.out.println("Testing subproject " + subproject.name() + " (" + subproject.path() + ")");
                testClassCount += test(
                        Subprojects.load(project.projectDir(), subproject),
                        visitedInChain,
                        false,
                        classFilters);
            }

            if (project.iniData().get("test") != null) {
                testClassCount += runProjectTests(project, classFilters);
            } else if (subprojects.isEmpty()) {
                System.out.println("No [test] in " + project.iniFile().getName() + "; skipping.");
            } else {
                System.out.println(
                        "No [test] in " + project.iniFile().getName() + "; testing subprojects only.");
            }
            if (isRoot && testClassCount == 0 && !classFilters.isEmpty()) {
                throw new IllegalStateException("No test class matching: " + String.join(", ", classFilters));
            }
            return testClassCount;
        });
    }

    private int runProjectTests(ProjectContext project, List<String> classFilters)
            throws IOException, InterruptedException {
        var plan = new TestCommandBuilder(project, classFilters).buildPlan();
        if (plan.testClassCount() == 0) {
            if (classFilters.isEmpty()) {
                System.err.println("Warning: no test classes found in " + project.iniFile().getName() + ".");
            }
            return 0;
        }
        printTestPlan(project.iniFile(), plan);
        var exitCode = runnerFactory.apply(project.projectDir()).run(plan.commands());
        if (exitCode != 0) {
            throw CommandFailedException.test(exitCode);
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
