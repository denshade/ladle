package thelaboflieven.info.build;


import thelaboflieven.info.CommandLine;
import thelaboflieven.info.download.JdkInstaller;
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

        JdkInstaller.ensureInstalled(projectDir, iniData);

        String parameters = javacSection.getOrDefault("parameters", "");
        var versionFlags = BuildConfig.javacVersionFlags(javacSection);
        String sources = sourcesSection.getOrDefault("paths", "");
        if (sources.isBlank()) {
            throw new IllegalStateException("Missing paths in [sources] section of INI file.");
        }

        String classpath = CompileClasspath.resolve(projectDir, iniData);

        var javacArguments = new ArrayList<String>();
        if (!classpath.isBlank()) {
            javacArguments.add("-cp");
            javacArguments.add(classpath);
        }
        javacArguments.addAll(versionFlags);
        javacArguments.addAll(CommandLine.splitParameters(parameters));

        var sourceFileCount = 0;
        for (String source: sources.split(",")) {
            var sourceRoot = new File(projectDir, source.trim());
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

        var javacExecutable = BuildConfig.javacExecutable(projectDir, iniData);
        var buildDirectory = BuildConfig.buildDirectory(iniData);
        var command = CommandLine.javacCommand(
                javacExecutable.getPath(),
                javacArguments,
                projectDir,
                buildDirectory + "/javac.args");

        return new BuildPlan(
                command,
                sourceFileCount,
                javacExecutable.getPath(),
                BuildConfig.javacParameterSummary(javacSection),
                classpath
        );
    }

}
