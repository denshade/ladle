package thelaboflieven.info.inifile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IniEnvironment {
    private static final Pattern BRACED = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)\\}");
    private static final Pattern SIMPLE = Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)");

    private IniEnvironment() {
    }

    public static String expand(String value) {
        if (value == null || value.isBlank()) {
            return value == null ? "" : value;
        }
        var expanded = expandBraced(value);
        return expandSimple(expanded);
    }

    public static boolean referencesEnvironment(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return BRACED.matcher(value).find() || SIMPLE.matcher(value).find();
    }

    private static String expandBraced(String value) {
        var matcher = BRACED.matcher(value);
        var buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(resolveVariable(matcher.group(1))));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String expandSimple(String value) {
        var matcher = SIMPLE.matcher(value);
        var buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(resolveVariable(matcher.group(1))));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String resolveVariable(String name) {
        var value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is not set.");
        }
        return value.trim();
    }
}
