package thelaboflieven.info.build;


import thelaboflieven.info.inifile.IniFileReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JavacCommandBuilder {

    private final Map<String, Map<String, String>> iniData;

    public JavacCommandBuilder(String iniFilePath) throws IOException {
        iniData = new IniFileReader().parseIniFile(iniFilePath);
    }

    public BuildPlan buildPlan() throws IOException {
        Map<String, String> javacSection = iniData.get("javac");
        Map<String, String> sourcesSection = iniData.get("sources");

        if (javacSection == null || sourcesSection == null) {
            throw new IllegalStateException("Missing [javac] or [sources] section in INI file.");
        }

        String javacPath = javacSection.getOrDefault("path", "javac");
        String parameters = javacSection.getOrDefault("parameters", "");
        String sources = sourcesSection.getOrDefault("paths", "");
        if (sources.isBlank()) {
            throw new IllegalStateException("Missing paths in [sources] section of INI file.");
        }

        List<String> command = new ArrayList<>();
        var javacPathFull = javacPath + File.separator + "bin" + File.separator + "javac.exe";
        if (!new File(javacPathFull).canRead()) {
            throw new IllegalStateException("Cannot read " + javacPathFull);
        }
        command.add(javacPathFull);

        if (!parameters.isBlank()) {
            command.add(parameters);
        }
        var sourceFileCount = 0;
        for (String source: sources.split(",")) {
            List<Path> javaFiles = Files.walk(new File(source.trim()).toPath())
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

        return new BuildPlan(String.join(" ", command), sourceFileCount, javacPathFull, parameters);
    }

}

