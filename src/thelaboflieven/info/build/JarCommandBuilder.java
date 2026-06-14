package thelaboflieven.info.build;

import thelaboflieven.info.inifile.IniFileReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

public class JarCommandBuilder {
    private static final Set<String> RESERVED_JAR_KEYS = Set.of("name", "directory", "manifest", "main-class");

    private final File projectDir;
    private final Map<String, Map<String, String>> iniData;

    public JarCommandBuilder(String iniFilePath) throws IOException {
        var iniFile = new File(iniFilePath);
        projectDir = iniFile.getParentFile();
        iniData = new IniFileReader().parseIniFile(iniFilePath);
    }

    public JarPlan planFor(File outputJar) throws IOException {
        String classesDir = BuildConfig.classesDirectory(iniData);
        var classesPath = new File(projectDir, classesDir);
        var jarTool = BuildConfig.jarExecutable(projectDir, iniData);
        if (!classesPath.isDirectory()) {
            throw new IllegalStateException("Missing compiled classes directory: " + classesPath.getPath());
        }

        var outputPath = outputJar.getAbsolutePath();
        var manifestFile = resolveManifestFile();
        String command;
        if (manifestFile == null) {
            command = jarTool.getPath() + " cf " + outputPath + " -C " + classesDir + " .";
        } else {
            var manifestPath = manifestArgument(manifestFile);
            command = jarTool.getPath() + " cfm " + outputPath + " " + manifestPath + " -C " + classesDir + " .";
        }
        return new JarPlan(command, outputPath, classesPath.getPath());
    }

    public File releaseOutputJar() {
        Map<String, String> jarSection = iniData.get("jar");
        if (jarSection == null) {
            throw new IllegalStateException("Missing [jar] section in INI file.");
        }

        var name = jarSection.getOrDefault("name", "").trim();
        if (name.isBlank()) {
            name = projectDir.getName();
        }
        if (name.isBlank()) {
            throw new IllegalStateException("Missing name in [jar] section of INI file.");
        }

        var outputDirectory = jarSection.getOrDefault("directory", "").trim();
        if (outputDirectory.isBlank()) {
            outputDirectory = buildOutputDirectory();
        }

        return new File(projectDir, outputDirectory + File.separator + name + ".jar").getAbsoluteFile();
    }

    private File resolveManifestFile() throws IOException {
        Map<String, String> jarSection = iniData.get("jar");
        if (jarSection == null) {
            return null;
        }

        var manifestPath = jarSection.getOrDefault("manifest", "").trim();
        if (!manifestPath.isBlank()) {
            var manifestFile = new File(projectDir, manifestPath);
            if (!manifestFile.canRead()) {
                throw new IllegalStateException("Cannot read manifest: " + manifestFile.getPath());
            }
            return manifestFile.getAbsoluteFile();
        }

        var manifestContent = buildManifestContent(jarSection);
        if (manifestContent.isBlank()) {
            return null;
        }

        var generatedManifest = new File(projectDir, buildOutputDirectory() + File.separator + "MANIFEST.MF");
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

    private String manifestArgument(File manifestFile) throws IOException {
        var projectPath = projectDir.getCanonicalFile().toPath();
        var manifestPath = manifestFile.getCanonicalFile().toPath();
        if (manifestPath.startsWith(projectPath)) {
            return projectPath.relativize(manifestPath).toString().replace('\\', '/');
        }
        return manifestFile.getPath();
    }

    private String buildOutputDirectory() {
        Map<String, String> buildSection = iniData.get("build");
        if (buildSection != null && !buildSection.getOrDefault("directory", "").isBlank()) {
            return buildSection.get("directory").trim();
        }
        return "build";
    }
}
