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
    public static final String JUNIT5_RUNNER = "org.junit.platform.console.ConsoleLauncher";
    public static final String JUNIT4_RUNNER = "org.junit.runner.JUnitCore";
    private static final String DEFAULT_OUTPUT = "build/test-classes";
    private static final String DEFAULT_FIXTURE_OUTPUT = "build/test-fixtures-classes";
    private static final String DEFAULT_CLASSPATH = "build/classes";

    private final ProjectContext project;
    private final List<String> classFilters;

    public TestCommandBuilder(String iniFilePath) throws IOException {
        this(ProjectContext.load(iniFilePath));
    }

    public TestCommandBuilder(ProjectContext project) {
        this(project, List.of());
    }

    public TestCommandBuilder(ProjectContext project, List<String> classFilters) {
        this.project = project;
        this.classFilters = classFilters == null ? List.of() : List.copyOf(classFilters);
    }

    public TestPlan buildPlan() throws IOException {
        Map<String, String> testSection = project.iniData().get("test");
        if (testSection == null) {
            throw new IllegalStateException("Missing [test] section in INI file.");
        }

        String sources = testSection.getOrDefault("sources", "");
        String classpath = testSection.getOrDefault("classpath", DEFAULT_CLASSPATH);
        String output = testSection.getOrDefault("output", DEFAULT_OUTPUT);
        var runner = resolveRunner(testSection);

        if (sources.isBlank()) {
            throw new IllegalStateException("Missing sources in [test] section of INI file.");
        }

        JdkInstaller.ensureInstalled(project.projectDir(), project.iniData());

        var fixtures = parseFixtures();
        var runtimeClasspathEntries = resolveRuntimeClasspathEntries(classpath);
        if (fixtures != null) {
            prependUnique(runtimeClasspathEntries, fixtures.output());
        }
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

        if (!classFilters.isEmpty()) {
            var filteredSources = new ArrayList<Path>();
            var filteredNames = new ArrayList<String>();
            for (int i = 0; i < testClassNames.size(); i++) {
                Path sourceFile = testSourceFiles.get(i);
                String className = testClassNames.get(i);
                if (matchesAnyFilter(className, sourceFile)) {
                    filteredSources.add(sourceFile);
                    filteredNames.add(className);
                }
            }
            testSourceFiles.clear();
            testSourceFiles.addAll(filteredSources);
            testClassNames.clear();
            testClassNames.addAll(filteredNames);
        }

        if (testClassNames.isEmpty()) {
            return new TestPlan(List.of(), 0, javaExecutable.getPath(), runtimeClasspath, runner);
        }

        var commands = new ArrayList<List<String>>();
        var buildDirectory = BuildConfig.buildDirectory(project.iniData());
        if (fixtures != null) {
            commands.add(javacCompileCommand(
                    javacExecutable,
                    fixtures.output(),
                    joinClasspath(fixtures.compileClasspathEntries()),
                    fixtures.sourceFiles(),
                    buildDirectory + "/test-fixtures-javac.args"));
        }
        commands.add(javacCompileCommand(
                javacExecutable,
                output,
                joinClasspath(compileClasspathEntries),
                testSourceFiles,
                buildDirectory + "/test-javac.args"));
        commands.add(testRunCommand(
                javaExecutable,
                runtimeClasspath,
                runner,
                testClassNames,
                buildDirectory + "/test-run.args"));

        return new TestPlan(commands, testClassNames.size(), javaExecutable.getPath(), runtimeClasspath, runner);
    }

    private List<String> testRunCommand(
            File javaExecutable,
            String runtimeClasspath,
            String runner,
            List<String> testClassNames,
            String argfileRelativePath
    ) throws IOException {
        var arguments = new ArrayList<String>();
        arguments.add("-cp");
        arguments.add(runtimeClasspath);
        arguments.add(runner);
        if (JUNIT4_RUNNER.equals(runner)) {
            arguments.addAll(testClassNames);
        } else {
            arguments.add("execute");
            arguments.add("--details-theme=ascii");
            for (var testClassName : testClassNames) {
                arguments.add("--select-class");
                arguments.add(testClassName);
            }
        }
        return CommandLine.javacCommand(
                javaExecutable.getPath(),
                arguments,
                project.projectDir(),
                argfileRelativePath);
    }

    private FixtureCompile parseFixtures() throws IOException {
        Map<String, String> fixtureSection = project.iniData().get("testfixtures");
        if (fixtureSection == null) {
            return null;
        }

        String sources = fixtureSection.getOrDefault("sources", "");
        if (sources.isBlank()) {
            throw new IllegalStateException("Missing sources in [testfixtures] section of INI file.");
        }

        String classpath = fixtureSection.getOrDefault("classpath", DEFAULT_CLASSPATH);
        String output = fixtureSection.getOrDefault("output", DEFAULT_FIXTURE_OUTPUT);
        var compileClasspathEntries = resolveCompileClasspathEntries(resolveRuntimeClasspathEntries(classpath));
        var sourceFiles = collectJavaFiles(sources, ".java", "Test fixture source path does not exist: ");
        if (sourceFiles.isEmpty()) {
            throw new IllegalStateException("No .java files found in [testfixtures].sources.");
        }
        return new FixtureCompile(output, compileClasspathEntries, sourceFiles);
    }

    private List<Path> collectJavaFiles(String sources, String fileSuffix, String missingPathMessage) throws IOException {
        var sourceFiles = new ArrayList<Path>();
        for (String sourceRoot : sources.split(",")) {
            var root = new File(project.projectDir(), sourceRoot.trim());
            if (!root.isDirectory()) {
                throw new IllegalStateException(missingPathMessage + root.getPath());
            }
            var rootPath = root.toPath().toAbsolutePath().normalize();
            List<Path> javaFiles = Files.walk(rootPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(fileSuffix))
                    .collect(Collectors.toList());
            sourceFiles.addAll(javaFiles);
        }
        return sourceFiles;
    }

    private List<String> javacCompileCommand(
            File javacExecutable,
            String output,
            String compileClasspath,
            List<Path> sourceFiles,
            String argfileRelativePath
    ) throws IOException {
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
        for (var sourceFile : sourceFiles) {
            compileArguments.add(sourceFile.toAbsolutePath().toString());
        }
        return CommandLine.javacCommand(
                javacExecutable.getPath(),
                compileArguments,
                project.projectDir(),
                argfileRelativePath);
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

    private static String resolveRunner(Map<String, String> testSection) {
        String runner = testSection.getOrDefault("runner", JUNIT5_RUNNER).trim();
        if (runner.isBlank() || runner.equals(JUNIT5_RUNNER)) {
            return JUNIT5_RUNNER;
        }
        if (runner.equals(JUNIT4_RUNNER)) {
            return JUNIT4_RUNNER;
        }
        throw new IllegalStateException(
                "Unsupported test runner: " + runner + ". Use " + JUNIT5_RUNNER
                        + " (JUnit 5) or " + JUNIT4_RUNNER + " (JUnit 4).");
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

    private static void prependUnique(List<String> entries, String value) {
        entries.remove(value);
        entries.add(0, value);
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

    private boolean matchesAnyFilter(String className, Path javaFile) {
        for (String filter : classFilters) {
            if (matchesClassFilter(project.projectDir(), className, javaFile, filter)) {
                return true;
            }
        }
        return false;
    }

    static boolean matchesClassFilter(File projectDir, String className, Path javaFile, String filter) {
        if (filter == null || filter.isBlank() || className == null) {
            return false;
        }
        String trimmed = filter.trim();
        if (trimmed.equals(className)) {
            return true;
        }
        int lastDot = className.lastIndexOf('.');
        String simpleName = lastDot < 0 ? className : className.substring(lastDot + 1);
        if (trimmed.equals(simpleName)) {
            return true;
        }
        if (javaFile == null) {
            return false;
        }
        Path file = javaFile.toAbsolutePath().normalize();
        String fileName = file.getFileName().toString();
        if (trimmed.equals(fileName)) {
            return true;
        }
        String unixFile = file.toString().replace('\\', '/');
        String unixFilter = trimmed.replace('\\', '/');
        if (unixFile.equals(unixFilter) || unixFile.endsWith("/" + unixFilter)) {
            return true;
        }
        try {
            Path filterPath = Path.of(trimmed);
            if (!filterPath.isAbsolute() && projectDir != null) {
                filterPath = projectDir.toPath().resolve(filterPath);
            }
            return file.equals(filterPath.toAbsolutePath().normalize());
        } catch (java.nio.file.InvalidPathException ex) {
            return false;
        }
    }

    private String toClassName(Path sourceRoot, Path javaFile) {
        var relative = sourceRoot.relativize(javaFile.toAbsolutePath().normalize());
        var classPath = relative.toString().replace(File.separatorChar, '.');
        return classPath.substring(0, classPath.length() - ".java".length());
    }

    private record FixtureCompile(
            String output,
            List<String> compileClasspathEntries,
            List<Path> sourceFiles
    ) {
    }
}
