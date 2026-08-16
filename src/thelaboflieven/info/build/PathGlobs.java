package thelaboflieven.info.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class PathGlobs {
    private final List<Pattern> includes;
    private final List<Pattern> excludes;

    private PathGlobs(List<Pattern> includes, List<Pattern> excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    public static PathGlobs fromJarSection(Map<String, String> jarSection) {
        if (jarSection == null) {
            return new PathGlobs(List.of(), List.of());
        }
        return new PathGlobs(
                compileAll(parseList(jarSection.get("include"))),
                compileAll(parseList(jarSection.get("exclude"))));
    }

    public boolean hasFilters() {
        return !includes.isEmpty() || !excludes.isEmpty();
    }

    public boolean accepts(String relativePath) {
        var path = normalize(relativePath);
        if (!includes.isEmpty() && !matchesAny(path, includes)) {
            return false;
        }
        return !matchesAny(path, excludes);
    }

    static List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        var patterns = new ArrayList<String>();
        for (var part : value.split(",")) {
            var pattern = normalize(part.trim());
            if (!pattern.isBlank()) {
                patterns.add(pattern);
            }
        }
        return patterns;
    }

    static boolean matches(String relativePath, String glob) {
        return toPattern(glob).matcher(normalize(relativePath)).matches();
    }

    private static boolean matchesAny(String path, List<Pattern> patterns) {
        for (var pattern : patterns) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    private static List<Pattern> compileAll(List<String> globs) {
        var patterns = new ArrayList<Pattern>(globs.size());
        for (var glob : globs) {
            patterns.add(toPattern(glob));
        }
        return List.copyOf(patterns);
    }

    static Pattern toPattern(String glob) {
        var regex = new StringBuilder("^");
        var normalized = normalize(glob);
        for (int i = 0; i < normalized.length(); i++) {
            var ch = normalized.charAt(i);
            if (ch == '*') {
                var doubleStar = i + 1 < normalized.length() && normalized.charAt(i + 1) == '*';
                if (doubleStar) {
                    i++;
                    if (i + 1 < normalized.length() && normalized.charAt(i + 1) == '/') {
                        regex.append("(?:.*/)?");
                        i++;
                    } else {
                        regex.append(".*");
                    }
                } else {
                    regex.append("[^/]*");
                }
            } else if (ch == '?') {
                regex.append("[^/]");
            } else if (isRegexMeta(ch)) {
                regex.append('\\').append(ch);
            } else {
                regex.append(ch);
            }
        }
        regex.append('$');
        return Pattern.compile(regex.toString());
    }

    private static boolean isRegexMeta(char ch) {
        return ".[]{}()+-^$|\\".indexOf(ch) >= 0;
    }

    static String normalize(String path) {
        var normalized = path.replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
