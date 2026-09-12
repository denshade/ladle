package thelaboflieven.info.build;

import thelaboflieven.info.CommandFailedException;
import thelaboflieven.info.CommandsRunner;
import thelaboflieven.info.ProjectContext;
import thelaboflieven.info.download.Dependencies;
import thelaboflieven.info.download.DependencyOrchestrator;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CompileOrchestrator {
    private final DependencyOrchestrator dependencyOrchestrator = new DependencyOrchestrator();

    public void compile(File iniFile) throws IOException, InterruptedException {
        compile(ProjectContext.load(iniFile.getAbsolutePath()), new HashSet<>(), null);
    }

    public void compile(ProjectContext project) throws IOException, InterruptedException {
        compile(project, new HashSet<>(), null);
    }

    private void compile(
            ProjectContext project,
            Set<String> visitedInChain,
            File publishJar
    ) throws IOException, InterruptedException {
        Subprojects.withCycleGuard(project, visitedInChain, () -> {
            new File(project.projectDir(), Dependencies.DIRECTORY).mkdirs();
            var subprojects = Subprojects.read(project.iniData());
            for (var subproject : subprojects) {
                System.out.println("Building subproject " + subproject.name() + " (" + subproject.path() + ")");
                compile(
                        Subprojects.load(project.projectDir(), subproject),
                        visitedInChain,
                        new File(project.projectDir(), Dependencies.filePath(subproject.name() + ".jar")));
            }

            var hasSources = BuildConfig.hasSources(project.iniData());
            if (!hasSources && subprojects.isEmpty() && publishJar == null) {
                throw new IllegalStateException(
                        "Missing [sources] section in INI file. Omit it only when [subproject] is present.");
            }

            dependencyOrchestrator.installProject(project);
            if (hasSources) {
                var plan = new JavacCommandBuilder(project).buildPlan();
                printBuildPlan(project.iniFile(), plan);
                var exitCode = new CommandsRunner(project.projectDir()).runCommand(plan.command());
                if (exitCode != 0) {
                    throw CommandFailedException.build(exitCode);
                }
            } else if (subprojects.isEmpty()) {
                System.out.println("No [sources] in " + project.iniFile().getName() + "; skipping compile.");
            } else {
                System.out.println(
                        "No [sources] in " + project.iniFile().getName() + "; compiling subprojects only.");
            }

            var copied = new ResourceCopier(project).copyResources();
            if (copied > 0) {
                System.out.println("Copying " + copied + " resource file(s) into classes directory...");
            }

            var classesDir = new File(project.projectDir(), BuildConfig.classesDirectory(project.iniData()));
            if (classesDir.isDirectory()) {
                var jarBuilder = new JarCommandBuilder(project);
                if (BuildConfig.hasJar(project.iniData())) {
                    jarBuilder.packageRelease();
                }
                if (publishJar != null) {
                    jarBuilder.packageTo(publishJar);
                }
            }

            System.out.println("Build successful.");
        });
    }

    private void printBuildPlan(File buildIni, BuildPlan plan) {
        System.out.println("Building from " + buildIni.getName());
        System.out.println("Compiling " + plan.sourceFileCount() + " Java source file(s)");
        System.out.println("  javac: " + plan.javacPath());
        if (!plan.parameters().isBlank()) {
            System.out.println("  parameters: " + plan.parameters());
        }
        if (!plan.classpath().isBlank()) {
            System.out.println("  classpath: " + plan.classpath());
        }
        if (!plan.processorPath().isBlank()) {
            System.out.println("  processorpath: " + plan.processorPath());
        }
        System.out.println("Running javac...");
    }
}
