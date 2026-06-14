package thelaboflieven.info.build;

import thelaboflieven.info.inifile.IniEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
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
        return IniEnvironment.expand(configuredRawJdkPath(iniData));
    }

    public static File jdkRoot(File projectDir, Map<String, Map<String, String>> iniData) {
        return jdkRoot(projectDir, configuredRawJdkPath(iniData));
    }

    public static File jdkRoot(File projectDir, String rawPath) {
        var expandedPath = IniEnvironment.expand(rawPath.trim());
        var jdkRoot = new File(expandedPath);
        if (!jdkRoot.isAbsolute()) {
            jdkRoot = new File(projectDir, expandedPath);
        }
        try {
            return jdkRoot.getCanonicalFile();
        } catch (IOException e) {
            return jdkRoot.getAbsoluteFile();
        }
    }

    public static String configuredRawJdkPath(Map<String, Map<String, String>> iniData) {
        Map<String, String> javacSection = iniData.get("javac");
        if (javacSection == null || javacSection.getOrDefault("path", "").isBlank()) {
            throw new IllegalStateException("Missing JDK path in [javac].path.");
        }
        return javacSection.get("path").trim();
    }

    public static File javacExecutable(File projectDir, Map<String, Map<String, String>> iniData) {
        return toolExecutable(jdkRoot(projectDir, iniData), "javac");
    }

    public static File javaExecutable(File projectDir, Map<String, Map<String, String>> iniData) {
        return toolExecutable(jdkRoot(projectDir, iniData), "java");
    }

    public static File jarExecutable(File projectDir, Map<String, Map<String, String>> iniData) {
        return toolExecutable(jdkRoot(projectDir, iniData), "jar");
    }

    public static File toolExecutable(File jdkRoot, String tool) {
        try {
            var executable = new File(jdkRoot, "bin" + File.separator + toolFileName(tool));
            if (!executable.canRead()) {
                throw new IllegalStateException("Cannot read " + executable.getPath());
            }
            return executable.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot resolve JDK tool path: " + tool, e);
        }
    }

    public static String toolFileName(String tool) {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            return tool + ".exe";
        }
        return tool;
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
