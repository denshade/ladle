package thelaboflieven.info.build;

import thelaboflieven.info.ProjectContext;
import thelaboflieven.info.CommandLine;
import thelaboflieven.info.download.Dependencies;
import thelaboflieven.info.download.JdkInstaller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JavacCommandBuilder {
    private final ProjectContext project;

    public JavacCommandBuilder(String iniFilePath) throws IOException {
        this(ProjectContext.load(iniFilePath));
    }

    public JavacCommandBuilder(ProjectContext project) {
        this.project = project;
    }

    public BuildPlan buildPlan() throws IOException {
        Map<String, String> javacSection = project.iniData().get("javac");
        Map<String, String> sourcesSection = project.iniData().get("sources");

        if (javacSection == null) {
            throw new IllegalStateException("Missing [javac] section in INI file.");
        }
        if (sourcesSection == null) {
            throw new IllegalStateException("Missing [sources] section in INI file.");
        }

        JdkInstaller.ensureInstalled(project.projectDir(), project.iniData());

        String parameters = javacSection.getOrDefault("parameters", "");
        var versionFlags = BuildConfig.javacVersionFlags(javacSection);
        String sources = sourcesSection.getOrDefault("paths", "");
        if (sources.isBlank()) {
            throw new IllegalStateException("Missing paths in [sources] section of INI file.");
        }

        String classpath = CompileClasspath.resolve(project.projectDir(), project.iniData());
        String processorPath = CompileClasspath.resolveProcessorPath(project.projectDir(), project.iniData());
        String processorClasses = Dependencies.annotationProcessorClasses(project.iniData());

        var javacArguments = new ArrayList<String>();
        if (!classpath.isBlank()) {
            javacArguments.add("-cp");
            javacArguments.add(classpath);
        }
        if (!processorPath.isBlank()) {
            javacArguments.add("-processorpath");
            javacArguments.add(processorPath);
        }
        if (!processorClasses.isBlank()) {
            javacArguments.add("-processor");
            javacArguments.add(processorClasses);
        }
        javacArguments.addAll(versionFlags);
        javacArguments.addAll(CommandLine.splitParameters(parameters));

        var sourceFileCount = 0;
        for (String source : sources.split(",")) {
            var sourceRoot = new File(project.projectDir(), source.trim());
            List<Path> javaFiles = Files.walk(sourceRoot.toPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            for (var javaFile : javaFiles) {
                javacArguments.add(javaFile.toAbsolutePath().toString());
                sourceFileCount++;
            }
        }
        if (sourceFileCount == 0) {
            throw new IllegalStateException("No .java files found in [sources].paths.");
        }

        var javacExecutable = BuildConfig.javacExecutable(project.projectDir(), project.iniData());
        var buildDirectory = BuildConfig.buildDirectory(project.iniData());
        var command = CommandLine.javacCommand(
                javacExecutable.getPath(),
                javacArguments,
                project.projectDir(),
                buildDirectory + "/javac.args");

        return new BuildPlan(
                command,
                sourceFileCount,
                javacExecutable.getPath(),
                BuildConfig.javacParameterSummary(javacSection),
                classpath,
                processorPath
        );
    }
}
