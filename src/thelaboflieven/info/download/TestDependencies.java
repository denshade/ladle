package thelaboflieven.info.download;

public final class TestDependencies {
    private TestDependencies() {
    }

    public static String fileName(String name, String url) {
        name = name.trim();
        if (name.endsWith(".jar")) {
            return name;
        }
        var lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == url.length() - 1) {
            throw new IllegalStateException("Invalid dependency URL for " + name + ": " + url);
        }
        return url.substring(lastSlash + 1);
    }

    public static String localPath(String name, String url) {
        return DependencyPaths.localPath(fileName(name, url));
    }
}
