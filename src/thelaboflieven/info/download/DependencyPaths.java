package thelaboflieven.info.download;

public final class DependencyPaths {
    public static final String DIRECTORY = "dependencies";

    private DependencyPaths() {
    }

    public static String localPath(String fileName) {
        return DIRECTORY + "/" + fileName;
    }
}
