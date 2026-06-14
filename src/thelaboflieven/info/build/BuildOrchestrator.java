package thelaboflieven.info.build;

import thelaboflieven.info.CommandsRunner;
import thelaboflieven.info.inifile.IniFileReader;
import thelaboflieven.info.download.DependencyPaths;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuildOrchestrator {
    private static final String DEFAULT_INI = "build.ini";

    public void build(File iniFile) throws IOException, InterruptedException {
        build(iniFile, null, null, new HashSet<>());
    }

    public void release(File iniFile) throws IOException, InterruptedException {
        build(iniFile);
        var projectDir = iniFile.getParentFile();
        var runner = new CommandsRunner(projectDir);
        packageReleaseJar(iniFile, runner);
        System.out.println("Release successful.");
    }

    private void build(
            File iniFile,
            File publishJarTo,
            String publishJarName,
            Set<String> visitedInChain
    ) throws IOException, InterruptedException {
        var canonicalPath = iniFile.getCanonicalPath();
        if (!visitedInChain.add(canonicalPath)) {
            throw new IllegalStateException("Circular subproject reference: " + iniFile.getPath());
        }

        try {
            var projectDir = iniFile.getParentFile();
            var runner = new CommandsRunner(projectDir);
            var iniData = new IniFileReader().parseIniFile(iniFile.getAbsolutePath());

            new File(projectDir, DependencyPaths.DIRECTORY).mkdirs();
            for (var subproject : readSubprojects(iniData)) {
                buildSubproject(projectDir, runner, subproject, visitedInChain);
            }

            var builder = new JavacCommandBuilder(iniFile.getAbsolutePath());
            var plan = builder.buildPlan();
            printBuildPlan(iniFile, plan);
            var exitCode = runner.run(List.of(plan.command()));
            if (exitCode != 0) {
                throw new BuildFailedException(exitCode);
            }

            var resourcePlan = new ResourceCopier(iniFile.getAbsolutePath()).copyResources();
            if (resourcePlan.fileCount() > 0) {
                printResourceCopyPlan(resourcePlan);
            }

            if (publishJarTo != null && publishJarName != null) {
                packageJar(iniFile, runner, new File(publishJarTo, publishJarName + ".jar"));
            }

            System.out.println("Build successful.");
        } finally {
            visitedInChain.remove(canonicalPath);
        }
    }

    private void buildSubproject(
            File projectDir,
            CommandsRunner runner,
            Subproject subproject,
            Set<String> visitedInChain
    ) throws IOException, InterruptedException {
        var subDir = new File(projectDir, subproject.path());
        var subIni = new File(subDir, DEFAULT_INI);
        if (!subIni.canRead()) {
            throw new IllegalStateException("Cannot read subproject build.ini: " + subIni.getPath());
        }

        System.out.println("Building subproject " + subproject.name() + " (" + subproject.path() + ")");
        var publishDir = new File(projectDir, DependencyPaths.DIRECTORY);
        build(subIni, publishDir, subproject.name(), visitedInChain);
    }

    private void packageReleaseJar(File iniFile, CommandsRunner runner)
            throws IOException, InterruptedException {
        var jarBuilder = new JarCommandBuilder(iniFile.getAbsolutePath());
        var outputJar = jarBuilder.releaseOutputJar();
        System.out.println("Packaging " + outputJar.getName() + "...");
        packageJar(iniFile, runner, outputJar);
    }

    private void packageJar(File iniFile, CommandsRunner runner, File outputJar)
            throws IOException, InterruptedException {
        outputJar.getParentFile().mkdirs();
        var jarBuilder = new JarCommandBuilder(iniFile.getAbsolutePath());
        var jarPlan = jarBuilder.planFor(outputJar);
        System.out.println("  jar: " + jarPlan.command());
        var exitCode = runner.run(List.of(jarPlan.command()));
        if (exitCode != 0) {
            throw new BuildFailedException(exitCode);
        }
        if (!outputJar.canRead()) {
            throw new IllegalStateException("Failed to create " + outputJar.getPath());
        }
        System.out.println("Created " + outputJar.getPath());
    }

    private void publishJar(File iniFile, CommandsRunner runner, File targetDir, String name)
            throws IOException, InterruptedException {
        System.out.println("Publishing " + name + ".jar to " + DependencyPaths.DIRECTORY + "/");
        packageJar(iniFile, runner, new File(targetDir, name + ".jar").getAbsoluteFile());
    }

    private List<Subproject> readSubprojects(Map<String, Map<String, String>> iniData) {
        Map<String, String> section = iniData.get("subproject");
        if (section == null) {
            return List.of();
        }

        var subprojects = new ArrayList<Subproject>();
        for (var entry : section.entrySet()) {
            var name = entry.getKey().trim();
            var path = entry.getValue().trim();
            if (name.isBlank() || path.isBlank()) {
                continue;
            }
            subprojects.add(new Subproject(name, path));
        }
        return subprojects;
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
        System.out.println("Running javac...");
    }

    private void printResourceCopyPlan(ResourceCopyPlan plan) {
        System.out.println("Copying " + plan.fileCount() + " resource file(s) into classes directory...");
    }
}
