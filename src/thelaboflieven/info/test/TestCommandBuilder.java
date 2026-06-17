package thelaboflieven.info.test;

import thelaboflieven.info.ProjectContext;
import thelaboflieven.info.build.BuildConfig;
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

public class TestCommandBuilder {
    private static final String DEFAULT_RUNNER = "org.junit.platform.console.ConsoleLauncher";
    private static final String DEFAULT_OUTPUT = "build/test-classes";

    private final ProjectContext project;

    public TestCommandBuilder(String iniFilePath) throws IOException {
        this(ProjectContext.load(iniFilePath));
    }

    public TestCommandBuilder(ProjectContext project) {
        this.project = project;
    }

    public TestPlan buildPlan() throws IOException {
        Map<String, String> testSection = project.iniData().get("test");
        if (testSection == null) {
            throw new IllegalStateException("Missing [test] section in INI file.");
        }

        String sources = testSection.getOrDefault("sources", "");
        String classpath = testSection.getOrDefault("classpath", "build/classes");
        String output = testSection.getOrDefault("output", DEFAULT_OUTPUT);
        validateRunner(testSection);

        if (sources.isBlank()) {
            throw new IllegalStateException("Missing sources in [test] section of INI file.");
        }

        JdkInstaller.ensureInstalled(project.projectDir(), project.iniData());

        var runtimeClasspathEntries = resolveRuntimeClasspathEntries(classpath);
        if (runtimeClasspathEntries.isEmpty()) {
            throw new IllegalStateException("Missing classpath in [test] section of INI file.");
        }

        var javacExecutable = resolveTool(testSection, "javac");
        var javaExecutable = resolveTool(testSection, "java");

        var compileClasspathEntries = resolveCompileClasspathEntries(runtimeClasspathEntries);
        var runtimeClasspath = joinClasspath(runtimeClasspathEntries, output);
        var testClassNames = new ArrayList<String>();
        var testSourceFiles = new ArrayList<Path>();

        for (String sourceRoot : sources.split(",")) {
            var root = new File(project.projectDir(), sourceRoot.trim());
            if (!root.isDirectory()) {
                throw new IllegalStateException("Test source path does not exist: " + root.getPath());
            }
            var rootPath = root.toPath().toAbsolutePath().normalize();
            List<Path> javaFiles = Files.walk(rootPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("Test.java"))
                    .collect(Collectors.toList());
            for (var javaFile : javaFiles) {
                testSourceFiles.add(javaFile);
                testClassNames.add(toClassName(rootPath, javaFile));
            }
        }

        if (testClassNames.isEmpty()) {
            return new TestPlan(List.of(), 0, javaExecutable.getPath(), runtimeClasspath, DEFAULT_RUNNER);
        }

        var commands = new ArrayList<List<String>>();
        var compileClasspath = joinClasspath(compileClasspathEntries);
        var compileArguments = new ArrayList<String>();
        compileArguments.add("-encoding");
        compileArguments.add("UTF-8");
        compileArguments.add("-d");
        compileArguments.add(output);
        compileArguments.add("-cp");
        compileArguments.add(compileClasspath);
        var javacSection = project.iniData().get("javac");
        if (javacSection != null) {
            compileArguments.addAll(BuildConfig.javacVersionFlags(javacSection));
        }
        for (var testSourceFile : testSourceFiles) {
            compileArguments.add(testSourceFile.toAbsolutePath().toString());
        }
        var buildDirectory = BuildConfig.buildDirectory(project.iniData());
        commands.add(CommandLine.javacCommand(
                javacExecutable.getPath(),
                compileArguments,
                project.projectDir(),
                buildDirectory + "/test-javac.args"));

        var runCommand = new ArrayList<String>();
        runCommand.add(javaExecutable.getPath());
        runCommand.add("-cp");
        runCommand.add(runtimeClasspath);
        runCommand.add(DEFAULT_RUNNER);
        runCommand.add("execute");
        runCommand.add("--details-theme=ascii");
        for (var testClassName : testClassNames) {
            runCommand.add("--select-class");
            runCommand.add(testClassName);
        }
        commands.add(runCommand);

        return new TestPlan(commands, testClassNames.size(), javaExecutable.getPath(), runtimeClasspath, DEFAULT_RUNNER);
    }

    private File resolveTool(Map<String, String> testSection, String tool) {
        if (!testSection.getOrDefault("path", "").isBlank()) {
            return BuildConfig.toolExecutable(
                    BuildConfig.jdkRoot(project.projectDir(), testSection.get("path")),
                    tool);
        }
        return switch (tool) {
            case "javac" -> BuildConfig.javacExecutable(project.projectDir(), project.iniData());
            case "java" -> BuildConfig.javaExecutable(project.projectDir(), project.iniData());
            default -> throw new IllegalArgumentException("Unknown tool: " + tool);
        };
    }

    private static void validateRunner(Map<String, String> testSection) {
        String runner = testSection.getOrDefault("runner", DEFAULT_RUNNER).trim();
        if (runner.isBlank()) {
            return;
        }
        if (runner.contains("JUnitCore")) {
            throw new IllegalStateException(
                    "JUnit 4 runner is not supported. Remove [test].runner from build.ini to use JUnit 5.");
        }
        if (!runner.equals(DEFAULT_RUNNER)) {
            throw new IllegalStateException(
                    "Unsupported test runner: " + runner + ". Use " + DEFAULT_RUNNER + ".");
        }
    }

    private List<String> resolveRuntimeClasspathEntries(String classpath) {
        return addDependencyPaths(splitEntries(classpath), project.iniData().get(Dependencies.TEST));
    }

    private List<String> resolveCompileClasspathEntries(List<String> runtimeEntries) {
        return addDependencyPaths(new ArrayList<>(runtimeEntries), project.iniData().get(Dependencies.COMPILE_ONLY));
    }

    private List<String> addDependencyPaths(List<String> entries, Map<String, String> section) {
        for (var path : Dependencies.localPathsFromSection(section)) {
            if (!entries.contains(path)) {
                entries.add(path);
            }
        }
        return entries;
    }

    private String joinClasspath(List<String> entries) {
        return String.join(String.valueOf(File.pathSeparatorChar), entries);
    }

    private String joinClasspath(List<String> entries, String output) {
        var runtimeEntries = new ArrayList<>(entries);
        runtimeEntries.add(output);
        return joinClasspath(runtimeEntries);
    }

    private List<String> splitEntries(String value) {
        var entries = new ArrayList<String>();
        for (String entry : value.split(",")) {
            entry = entry.trim();
            if (!entry.isBlank()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private String toClassName(Path sourceRoot, Path javaFile) {
        var relative = sourceRoot.relativize(javaFile.toAbsolutePath().normalize());
        var classPath = relative.toString().replace(File.separatorChar, '.');
        return classPath.substring(0, classPath.length() - ".java".length());
    }
}
