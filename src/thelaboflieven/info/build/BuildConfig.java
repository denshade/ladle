package thelaboflieven.info.build;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class BuildConfig {
    private static final String DEFAULT_CLASSES_DIR = "build/classes";

    private BuildConfig() {
    }

    public static String classesDirectory(Map<String, Map<String, String>> iniData) {
        Map<String, String> javacSection = iniData.get("javac");
        if (javacSection == null) {
            return DEFAULT_CLASSES_DIR;
        }
        return findFlagValue(javacSection.getOrDefault("parameters", ""), "-d", DEFAULT_CLASSES_DIR);
    }

    public static String configuredJdkPath(Map<String, Map<String, String>> iniData) {
        Map<String, String> javacSection = iniData.get("javac");
        if (javacSection == null || javacSection.getOrDefault("path", "").isBlank()) {
            throw new IllegalStateException("Missing JDK path in [javac].path.");
        }
        return javacSection.get("path").trim();
    }

    public static File javacExecutable(File projectDir, Map<String, Map<String, String>> iniData) {
        return toolExecutable(projectDir, iniData, "javac.exe");
    }

    public static File javaExecutable(File projectDir, Map<String, Map<String, String>> iniData) {
        return toolExecutable(projectDir, iniData, "java.exe");
    }

    public static File jarExecutable(File projectDir, Map<String, Map<String, String>> iniData) {
        return toolExecutable(projectDir, iniData, "jar.exe");
    }

    private static File toolExecutable(File projectDir, Map<String, Map<String, String>> iniData, String toolName) {
        try {
            var executable = new File(new File(projectDir, configuredJdkPath(iniData)), "bin" + File.separator + toolName);
            if (!executable.canRead()) {
                throw new IllegalStateException("Cannot read " + executable.getPath());
            }
            return executable.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot resolve JDK tool path: " + toolName, e);
        }
    }

    private static String findFlagValue(String parameters, String flag, String defaultValue) {
        if (parameters.isBlank()) {
            return defaultValue;
        }
        var parts = parameters.trim().split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals(flag)) {
                return parts[i + 1];
            }
        }
        return defaultValue;
    }
}
