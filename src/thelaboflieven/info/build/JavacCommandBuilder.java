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

    public String buildCommand() throws IOException {
        Map<String, String> javacSection = iniData.get("javac");
        Map<String, String> sourcesSection = iniData.get("sources");

        if (javacSection == null || sourcesSection == null) {
            throw new IllegalStateException("Missing [javac] or [sources] section in INI file.");
        }

        String javacPath = javacSection.getOrDefault("path", "javac");
        String parameters = javacSection.getOrDefault("parameters", "");
        String sources = sourcesSection.getOrDefault("paths", "");

        // Build command
        List<String> command = new ArrayList<>();
        var javacPathFull = javacPath + File.separator + "bin" + File.separator + "javac.exe";
        if (!new File(javacPathFull).canRead()) {
            System.err.println("Cannot read " + javacPathFull);
            System.exit(2); //TODO; throw exception, not stop execution
        }
        command.add(javacPathFull);

        if (!parameters.isBlank()) {
            command.add(parameters);
        }
        for (String source: sources.split(",")) {
            List<Path> javaFiles = Files.walk(new File(source).toPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            for (var javaFile : javaFiles) {
                command.add(javaFile.toAbsolutePath().toString());
            }
        }

        return String.join(" ", command);
    }

}

