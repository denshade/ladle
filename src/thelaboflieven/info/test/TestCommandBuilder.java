package thelaboflieven.info.test;

import thelaboflieven.info.build.BuildConfig;
import thelaboflieven.info.download.JdkInstaller;
import thelaboflieven.info.inifile.IniFileReader;
import thelaboflieven.info.download.TestDependencies;

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

    private final File projectDir;
    private final Map<String, Map<String, String>> iniData;

    public TestCommandBuilder(String iniFilePath) throws IOException {
        var iniFile = new File(iniFilePath);
        projectDir = iniFile.getParentFile();
        iniData = new IniFileReader().parseIniFile(iniFilePath);
    }

    public TestPlan buildPlan() throws IOException {
        Map<String, String> testSection = iniData.get("test");
        if (testSection == null) {
            throw new IllegalStateException("Missing [test] section in INI file.");
        }

        String sources = testSection.getOrDefault("sources", "");
        String classpath = testSection.getOrDefault("classpath", "build/classes");
        String output = testSection.getOrDefault("output", DEFAULT_OUTPUT);
        String runner = resolveRunner(testSection);

        if (sources.isBlank()) {
            throw new IllegalStateException("Missing sources in [test] section of INI file.");
        }

        JdkInstaller.ensureInstalled(projectDir, iniData);

        var runtimeClasspathEntries = resolveRuntimeClasspathEntries(classpath);
        if (runtimeClasspathEntries.isEmpty()) {
            throw new IllegalStateException("Missing classpath in [test] section of INI file.");
        }

        var javacExecutable = resolveJavacExecutable(testSection);
        var javaExecutable = resolveJavaExecutable(testSection);

        var compileClasspathEntries = resolveCompileClasspathEntries(runtimeClasspathEntries);
        var runtimeClasspath = joinClasspath(runtimeClasspathEntries, output);
        var testClassNames = new ArrayList<String>();
        var testSourceFiles = new ArrayList<Path>();

        for (String sourceRoot : sources.split(",")) {
            var root = new File(projectDir, sourceRoot.trim());
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
            return new TestPlan(List.of(), 0, javaExecutable.getPath(), runtimeClasspath, runner);
        }

        var commands = new ArrayList<String>();
        var compileClasspath = joinClasspath(compileClasspathEntries);
        var compileCommand = new ArrayList<String>();
        compileCommand.add(javacExecutable.getPath());
        compileCommand.add("-encoding");
        compileCommand.add("UTF-8");
        compileCommand.add("-d");
        compileCommand.add(output);
        compileCommand.add("-cp");
        compileCommand.add(compileClasspath);
        var javacSection = iniData.get("javac");
        if (javacSection != null) {
            compileCommand.addAll(BuildConfig.javacVersionFlags(javacSection));
        }
        for (var testSourceFile : testSourceFiles) {
            compileCommand.add(testSourceFile.toAbsolutePath().toString());
        }
        commands.add(String.join(" ", compileCommand));

        var runCommand = new ArrayList<String>();
        runCommand.add(javaExecutable.getPath());
        runCommand.add("-cp");
        runCommand.add(runtimeClasspath);
        runCommand.add(runner);
        runCommand.add("execute");
        runCommand.add("--details-theme=ascii");
        for (var testClassName : testClassNames) {
            runCommand.add("--select-class");
            runCommand.add(testClassName);
        }
        commands.add(String.join(" ", runCommand));

        return new TestPlan(commands, testClassNames.size(), javaExecutable.getPath(), runtimeClasspath, runner);
    }

    private File resolveJavacExecutable(Map<String, String> testSection) {
        if (!testSection.getOrDefault("path", "").isBlank()) {
            return BuildConfig.toolExecutable(
                    BuildConfig.jdkRoot(projectDir, testSection.get("path")),
                    "javac");
        }
        return BuildConfig.javacExecutable(projectDir, iniData);
    }

    private File resolveJavaExecutable(Map<String, String> testSection) {
        if (!testSection.getOrDefault("path", "").isBlank()) {
            return BuildConfig.toolExecutable(
                    BuildConfig.jdkRoot(projectDir, testSection.get("path")),
                    "java");
        }
        return BuildConfig.javaExecutable(projectDir, iniData);
    }

    private String resolveRunner(Map<String, String> testSection) {
        String runner = testSection.getOrDefault("runner", DEFAULT_RUNNER).trim();
        if (runner.isBlank()) {
            return DEFAULT_RUNNER;
        }
        if (runner.contains("JUnitCore")) {
            throw new IllegalStateException(
                    "JUnit 4 runner is not supported. Remove [test].runner from build.ini to use JUnit 5.");
        }
        if (!runner.equals(DEFAULT_RUNNER)) {
            throw new IllegalStateException(
                    "Unsupported test runner: " + runner + ". Use " + DEFAULT_RUNNER + ".");
        }
        return DEFAULT_RUNNER;
    }

    private List<String> resolveRuntimeClasspathEntries(String classpath) {
        return addDependencySection(splitEntries(classpath), iniData.get("testdependencies"));
    }

    private List<String> resolveCompileClasspathEntries(List<String> runtimeEntries) {
        return addDependencySection(new ArrayList<>(runtimeEntries), iniData.get("compileonlydependencies"));
    }

    private List<String> addDependencySection(List<String> entries, Map<String, String> dependencies) {
        if (dependencies == null) {
            return entries;
        }
        for (var entry : dependencies.entrySet()) {
            var name = entry.getKey().trim();
            var url = entry.getValue().trim();
            if (name.isBlank() || url.isBlank()) {
                continue;
            }
            var path = TestDependencies.localPath(name, url);
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
