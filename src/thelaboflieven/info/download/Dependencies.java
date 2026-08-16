package thelaboflieven.info.download;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Dependencies {
    public static final String IMPLEMENTATION = "dependencies";
    public static final String COMPILE_ONLY = "compileonlydependencies";
    public static final String TEST = "testdependencies";
    public static final String ANNOTATION_PROCESSOR = "annotationprocessor";
    private static final Set<String> ANNOTATION_PROCESSOR_RESERVED_KEYS = Set.of("processor");

    private Dependencies() {
    }

    public static List<String> implementationPaths(Map<String, Map<String, String>> iniData) {
        return localPaths(iniData, IMPLEMENTATION);
    }

    public static List<String> compileOnlyPaths(Map<String, Map<String, String>> iniData) {
        return localPaths(iniData, COMPILE_ONLY);
    }

    public static List<String> testPaths(Map<String, Map<String, String>> iniData) {
        return localPaths(iniData, TEST);
    }

    public static List<String> annotationProcessorPaths(Map<String, Map<String, String>> iniData) {
        return localPathsFromSection(
                withoutReservedKeys(iniData.get(ANNOTATION_PROCESSOR), ANNOTATION_PROCESSOR_RESERVED_KEYS));
    }

    public static String annotationProcessorClasses(Map<String, Map<String, String>> iniData) {
        Map<String, String> section = iniData.get(ANNOTATION_PROCESSOR);
        if (section == null) {
            return "";
        }
        return section.getOrDefault("processor", "").trim();
    }

    public static List<String> localPaths(Map<String, Map<String, String>> iniData, String sectionName) {
        return localPathsFromSection(iniData.get(sectionName));
    }

    public static List<String> localPathsFromSection(Map<String, String> section) {
        if (section == null) {
            return List.of();
        }

        var paths = new ArrayList<String>();
        for (var entry : section.entrySet()) {
            var name = entry.getKey().trim();
            var url = entry.getValue().trim();
            if (name.isBlank() || url.isBlank()) {
                continue;
            }
            paths.add(localPath(name, url));
        }
        return paths;
    }

    public static List<DependencyArtifact> artifacts(Map<String, Map<String, String>> iniData) {
        var artifacts = new ArrayList<DependencyArtifact>();
        addArtifacts(artifacts, iniData.get(IMPLEMENTATION));
        addArtifacts(artifacts, iniData.get(COMPILE_ONLY));
        addArtifacts(artifacts, iniData.get(TEST));
        addArtifacts(
                artifacts,
                withoutReservedKeys(iniData.get(ANNOTATION_PROCESSOR), ANNOTATION_PROCESSOR_RESERVED_KEYS));
        return artifacts;
    }

    public static String fileName(String name, String url) {
        name = name.trim();
        if (name.endsWith(".jar")) {
            return name;
        }
        var withoutQuery = url;
        var queryIndex = withoutQuery.indexOf('?');
        if (queryIndex >= 0) {
            withoutQuery = withoutQuery.substring(0, queryIndex);
        }
        var lastSlash = withoutQuery.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == withoutQuery.length() - 1) {
            throw new IllegalStateException("Invalid dependency URL for " + name + ": " + url);
        }
        return withoutQuery.substring(lastSlash + 1);
    }

    public static String fileNameFromUrl(String url) {
        var withoutQuery = url;
        var queryIndex = withoutQuery.indexOf('?');
        if (queryIndex >= 0) {
            withoutQuery = withoutQuery.substring(0, queryIndex);
        }
        var lastSlash = withoutQuery.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == withoutQuery.length() - 1) {
            throw new IllegalStateException("Invalid dependency URL: " + url);
        }
        return withoutQuery.substring(lastSlash + 1);
    }

    public static String localPath(String name, String url) {
        return DependencyPaths.localPath(fileName(name, url));
    }

    private static void addArtifacts(List<DependencyArtifact> artifacts, Map<String, String> section) {
        if (section == null) {
            return;
        }

        for (var entry : section.entrySet()) {
            var name = entry.getKey().trim();
            var url = entry.getValue().trim();
            if (name.isBlank() || url.isBlank()) {
                continue;
            }
            artifacts.add(new DependencyArtifact(url, fileName(name, url)));
        }
    }

    private static Map<String, String> withoutReservedKeys(Map<String, String> section, Set<String> reserved) {
        if (section == null) {
            return null;
        }
        var filtered = new LinkedHashMap<String, String>();
        for (var entry : section.entrySet()) {
            if (!reserved.contains(entry.getKey().trim())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }
}
