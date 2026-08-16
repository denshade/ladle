package thelaboflieven.info.build;

import thelaboflieven.info.CommandLine;
import thelaboflieven.info.ProjectContext;
import thelaboflieven.info.ProjectPaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JarCommandBuilder {
    private static final Set<String> RESERVED_JAR_KEYS = Set.of(
            "name", "directory", "manifest", "main-class", "include", "exclude");

    private final ProjectContext project;

    public JarCommandBuilder(String iniFilePath) throws IOException {
        this(ProjectContext.load(iniFilePath));
    }

    public JarCommandBuilder(ProjectContext project) {
        this.project = project;
    }

    public JarPlan planFor(File outputJar) throws IOException {
        String classesDir = BuildConfig.classesDirectory(project.iniData());
        var classesPath = new File(project.projectDir(), classesDir);
        var jarTool = BuildConfig.jarExecutable(project.projectDir(), project.iniData());
        if (!classesPath.isDirectory()) {
            throw new IllegalStateException("Missing compiled classes directory: " + classesPath.getPath());
        }

        var outputPath = outputJar.getAbsolutePath();
        var manifestFile = resolveManifestFile();
        var jarSection = project.iniData().get("jar");
        var entries = selectEntries(classesPath, PathGlobs.fromJarSection(jarSection));

        var jarArguments = new ArrayList<String>();
        if (manifestFile == null) {
            jarArguments.add("cf");
        } else {
            jarArguments.add("cfm");
        }
        jarArguments.add(outputPath);
        if (manifestFile != null) {
            jarArguments.add(ProjectPaths.relativeTo(project.projectDir(), manifestFile));
        }
        jarArguments.add("-C");
        jarArguments.add(classesDir);
        jarArguments.addAll(entries);

        var command = CommandLine.javacCommand(
                jarTool.getPath(),
                jarArguments,
                project.projectDir(),
                BuildConfig.buildDirectory(project.iniData()) + "/jar.args");
        return new JarPlan(command, outputPath, classesPath.getPath());
    }

    private List<String> selectEntries(File classesPath, PathGlobs globs) throws IOException {
        if (!globs.hasFilters()) {
            return List.of(".");
        }

        var classesRoot = classesPath.toPath().toAbsolutePath().normalize();
        try (var stream = Files.walk(classesRoot)) {
            var entries = stream
                    .filter(Files::isRegularFile)
                    .map(path -> relativeJarPath(classesRoot, path))
                    .filter(globs::accepts)
                    .sorted()
                    .toList();
            if (entries.isEmpty()) {
                throw new IllegalStateException("No files matched [jar] include/exclude patterns.");
            }
            return entries;
        }
    }

    private static String relativeJarPath(Path classesRoot, Path file) {
        return classesRoot.relativize(file).toString().replace('\\', '/');
    }

    public File releaseOutputJar() {
        Map<String, String> jarSection = project.iniData().get("jar");
        if (jarSection == null) {
            throw new IllegalStateException("Missing [jar] section in INI file.");
        }

        var name = jarSection.getOrDefault("name", "").trim();
        if (name.isBlank()) {
            name = project.projectDir().getName();
        }
        if (name.isBlank()) {
            throw new IllegalStateException("Missing name in [jar] section of INI file.");
        }

        var outputDirectory = jarSection.getOrDefault("directory", "").trim();
        if (outputDirectory.isBlank()) {
            outputDirectory = BuildConfig.buildDirectory(project.iniData());
        }

        return new File(project.projectDir(), outputDirectory + File.separator + name + ".jar").getAbsoluteFile();
    }

    private File resolveManifestFile() throws IOException {
        Map<String, String> jarSection = project.iniData().get("jar");
        if (jarSection == null) {
            return null;
        }

        var manifestPath = jarSection.getOrDefault("manifest", "").trim();
        if (!manifestPath.isBlank()) {
            var manifestFile = new File(project.projectDir(), manifestPath);
            if (!manifestFile.canRead()) {
                throw new IllegalStateException("Cannot read manifest: " + manifestFile.getPath());
            }
            return manifestFile.getAbsoluteFile();
        }

        var manifestContent = buildManifestContent(jarSection);
        if (manifestContent.isBlank()) {
            return null;
        }

        var generatedManifest = new File(
                project.projectDir(),
                BuildConfig.buildDirectory(project.iniData()) + File.separator + "MANIFEST.MF");
        generatedManifest.getParentFile().mkdirs();
        Files.writeString(generatedManifest.toPath(), manifestContent);
        return generatedManifest.getAbsoluteFile();
    }

    private String buildManifestContent(Map<String, String> jarSection) {
        var builder = new StringBuilder();
        appendManifestAttribute(builder, "Main-Class", jarSection.getOrDefault("main-class", "").trim());
        for (var entry : jarSection.entrySet()) {
            if (RESERVED_JAR_KEYS.contains(entry.getKey())) {
                continue;
            }
            appendManifestAttribute(builder, toManifestAttributeName(entry.getKey()), entry.getValue().trim());
        }
        return builder.toString();
    }

    private static void appendManifestAttribute(StringBuilder builder, String name, String value) {
        if (value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(name).append(": ").append(value).append('\n');
    }

    private static String toManifestAttributeName(String key) {
        var parts = key.split("-");
        var builder = new StringBuilder();
        for (var part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('-');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
