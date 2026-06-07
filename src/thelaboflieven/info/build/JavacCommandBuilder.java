package thelaboflieven.info.build;


import thelaboflieven.info.download.DependencyPaths;
import thelaboflieven.info.inifile.IniFileReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JavacCommandBuilder {

    private final File projectDir;
    private final Map<String, Map<String, String>> iniData;

    public JavacCommandBuilder(String iniFilePath) throws IOException {
        var iniFile = new File(iniFilePath);
        projectDir = iniFile.getParentFile();
        iniData = new IniFileReader().parseIniFile(iniFilePath);
    }

    public BuildPlan buildPlan() throws IOException {
        Map<String, String> javacSection = iniData.get("javac");
        Map<String, String> sourcesSection = iniData.get("sources");

        if (javacSection == null || sourcesSection == null) {
            throw new IllegalStateException("Missing [javac] or [sources] section in INI file.");
        }

        String parameters = javacSection.getOrDefault("parameters", "");
        String sources = sourcesSection.getOrDefault("paths", "");
        if (sources.isBlank()) {
            throw new IllegalStateException("Missing paths in [sources] section of INI file.");
        }

        String subprojectClasspath = resolveSubprojectClasspath();

        List<String> command = new ArrayList<>();
        var javacExecutable = BuildConfig.javacExecutable(projectDir, iniData);
        command.add(javacExecutable.getPath());

        if (!subprojectClasspath.isBlank()) {
            command.add("-cp");
            command.add(subprojectClasspath);
        }
        if (!parameters.isBlank()) {
            command.add(parameters);
        }
        var sourceFileCount = 0;
        for (String source: sources.split(",")) {
            var sourceRoot = new File(projectDir, source.trim());
            List<Path> javaFiles = Files.walk(sourceRoot.toPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            for (var javaFile : javaFiles) {
                command.add(javaFile.toAbsolutePath().toString());
                sourceFileCount++;
            }
        }
        if (sourceFileCount == 0) {
            throw new IllegalStateException("No .java files found in [sources].paths.");
        }

        return new BuildPlan(
                String.join(" ", command),
                sourceFileCount,
                javacExecutable.getPath(),
                parameters,
                subprojectClasspath
        );
    }

    private String resolveSubprojectClasspath() {
        Map<String, String> subprojects = iniData.get("subproject");
        if (subprojects == null) {
            return "";
        }

        var entries = new ArrayList<String>();
        for (String name : subprojects.keySet()) {
            name = name.trim();
            if (name.isBlank()) {
                continue;
            }
            var jarFile = new File(projectDir, DependencyPaths.localPath(name + ".jar"));
            if (!jarFile.canRead()) {
                throw new IllegalStateException("Missing subproject jar: " + jarFile.getPath());
            }
            entries.add(DependencyPaths.localPath(name + ".jar"));
        }
        return String.join(String.valueOf(File.pathSeparatorChar), entries);
    }

}
