package thelaboflieven.info.build;

import thelaboflieven.info.download.CompileOnlyDependencies;
import thelaboflieven.info.download.DependencyPaths;
import thelaboflieven.info.download.ImplementationDependencies;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CompileClasspath {
    private CompileClasspath() {
    }

    public static String resolve(File projectDir, Map<String, Map<String, String>> iniData) {
        var entries = new ArrayList<String>();
        entries.addAll(subprojectEntries(projectDir, iniData));
        entries.addAll(implementationEntries(projectDir, iniData));
        entries.addAll(compileOnlyEntries(projectDir, iniData));
        return String.join(String.valueOf(File.pathSeparatorChar), entries);
    }

    private static List<String> subprojectEntries(File projectDir, Map<String, Map<String, String>> iniData) {
        Map<String, String> subprojects = iniData.get("subproject");
        if (subprojects == null) {
            return List.of();
        }

        var entries = new ArrayList<String>();
        for (String name : subprojects.keySet()) {
            name = name.trim();
            if (name.isBlank()) {
                continue;
            }
            var relativePath = DependencyPaths.localPath(name + ".jar");
            var jarFile = new File(projectDir, relativePath);
            if (!jarFile.canRead()) {
                throw new IllegalStateException("Missing subproject jar: " + jarFile.getPath());
            }
            entries.add(relativePath);
        }
        return entries;
    }

    private static List<String> implementationEntries(File projectDir, Map<String, Map<String, String>> iniData) {
        return dependencyEntries(projectDir, ImplementationDependencies.localPaths(iniData));
    }

    private static List<String> compileOnlyEntries(File projectDir, Map<String, Map<String, String>> iniData) {
        return dependencyEntries(projectDir, CompileOnlyDependencies.localPaths(iniData));
    }

    private static List<String> dependencyEntries(File projectDir, List<String> relativePaths) {
        var entries = new ArrayList<String>();
        for (var relativePath : relativePaths) {
            var jarFile = new File(projectDir, relativePath);
            if (!jarFile.canRead()) {
                throw new IllegalStateException("Missing dependency jar: " + jarFile.getPath());
            }
            entries.add(relativePath);
        }
        return entries;
    }
}
