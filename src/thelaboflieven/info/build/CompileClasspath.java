package thelaboflieven.info.build;

import thelaboflieven.info.download.Dependencies;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CompileClasspath {
    private CompileClasspath() {
    }

    public static String resolve(File projectDir, Map<String, Map<String, String>> iniData) {
        var entries = new ArrayList<String>();
        entries.addAll(runtimeJarPaths(projectDir, iniData));
        entries.addAll(dependencyEntries(projectDir, Dependencies.compileOnlyPaths(iniData)));
        return String.join(String.valueOf(File.pathSeparatorChar), entries);
    }

    public static List<String> runtimeJarPaths(File projectDir, Map<String, Map<String, String>> iniData) {
        var entries = new ArrayList<String>();
        entries.addAll(subprojectEntries(projectDir, iniData));
        entries.addAll(dependencyEntries(projectDir, Dependencies.implementationPaths(iniData)));
        return entries;
    }

    public static String resolveProcessorPath(File projectDir, Map<String, Map<String, String>> iniData) {
        return String.join(
                String.valueOf(File.pathSeparatorChar),
                dependencyEntries(projectDir, Dependencies.annotationProcessorPaths(iniData)));
    }

    private static List<String> subprojectEntries(File projectDir, Map<String, Map<String, String>> iniData) {
        var entries = new ArrayList<String>();
        for (var subproject : Subprojects.read(iniData)) {
            var relativePath = Dependencies.filePath(subproject.name() + ".jar");
            entries.add(requireReadable(projectDir, relativePath, "Missing subproject jar: "));
        }
        return entries;
    }

    private static List<String> dependencyEntries(File projectDir, List<String> relativePaths) {
        var entries = new ArrayList<String>();
        for (var relativePath : relativePaths) {
            entries.add(requireReadable(projectDir, relativePath, "Missing dependency jar: "));
        }
        return entries;
    }

    private static String requireReadable(File projectDir, String relativePath, String missingPrefix) {
        var jarFile = new File(projectDir, relativePath);
        if (!jarFile.canRead()) {
            throw new IllegalStateException(missingPrefix + jarFile.getPath());
        }
        return relativePath;
    }
}
