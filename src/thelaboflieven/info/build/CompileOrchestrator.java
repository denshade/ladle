package thelaboflieven.info.build;

import thelaboflieven.info.CommandsRunner;
import thelaboflieven.info.ProjectContext;
import thelaboflieven.info.download.DependencyPaths;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompileOrchestrator {
    private final JarPackager jarPackager;

    public CompileOrchestrator() {
        this(new JarPackager());
    }

    CompileOrchestrator(JarPackager jarPackager) {
        this.jarPackager = jarPackager;
    }

    public void compile(File iniFile) throws IOException, InterruptedException {
        compile(ProjectContext.load(iniFile.getAbsolutePath()), new HashSet<>(), null);
    }

    public void compile(ProjectContext project) throws IOException, InterruptedException {
        compile(project, new HashSet<>(), null);
    }

    private void compile(
            ProjectContext project,
            Set<String> visitedInChain,
            SubprojectPublish publish
    ) throws IOException, InterruptedException {
        var canonicalPath = project.iniFile().getCanonicalPath();
        if (!visitedInChain.add(canonicalPath)) {
            throw new IllegalStateException("Circular subproject reference: " + project.iniFile().getPath());
        }

        try {
            new File(project.projectDir(), DependencyPaths.DIRECTORY).mkdirs();
            var subprojects = Subprojects.read(project.iniData());
            for (var subproject : subprojects) {
                compileSubproject(project.projectDir(), subproject, visitedInChain);
            }

            if (BuildConfig.hasSources(project.iniData())) {
                var plan = new JavacCommandBuilder(project).buildPlan();
                printBuildPlan(project.iniFile(), plan);
                var runner = new CommandsRunner(project.projectDir());
                var exitCode = runner.run(List.of(plan.command()));
                if (exitCode != 0) {
                    throw new BuildFailedException(exitCode);
                }
            } else if (subprojects.isEmpty()) {
                throw new IllegalStateException(
                        "Missing [sources] section in INI file. Omit it only when [subproject] is present.");
            } else {
                System.out.println(
                        "No [sources] in " + project.iniFile().getName() + "; compiling subprojects only.");
            }

            var resourcePlan = new ResourceCopier(project).copyResources();
            if (resourcePlan.fileCount() > 0) {
                printResourceCopyPlan(resourcePlan);
            }

            if (publish != null) {
                jarPackager.packageJar(
                        project,
                        new File(publish.directory(), publish.name() + ".jar"));
            }

            System.out.println("Build successful.");
        } finally {
            visitedInChain.remove(canonicalPath);
        }
    }

    private void compileSubproject(
            File projectDir,
            Subproject subproject,
            Set<String> visitedInChain
    ) throws IOException, InterruptedException {
        System.out.println("Building subproject " + subproject.name() + " (" + subproject.path() + ")");
        var publishDir = new File(projectDir, DependencyPaths.DIRECTORY);
        compile(
                Subprojects.load(projectDir, subproject),
                visitedInChain,
                new SubprojectPublish(publishDir, subproject.name()));
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

    private void printResourceCopyPlan(ResourceCopyPlan plan) {
        System.out.println("Copying " + plan.fileCount() + " resource file(s) into classes directory...");
    }

    private record SubprojectPublish(File directory, String name) {
    }
}
