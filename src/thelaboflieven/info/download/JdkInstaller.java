package thelaboflieven.info.download;

import thelaboflieven.info.build.BuildConfig;
import thelaboflieven.info.download.Dependencies;
import thelaboflieven.info.inifile.IniEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public final class JdkInstaller {
    private static final String STAGING_DIR = ".jdk-staging";

    private JdkInstaller() {
    }

    public static boolean isConfigured(Map<String, Map<String, String>> iniData) {
        Map<String, String> javacSection = iniData.get("javac");
        if (javacSection == null) {
            return false;
        }
        return !javacSection.getOrDefault("path", "").isBlank() || hasDownloadUrl(javacSection);
    }

    public static void ensureInstalled(File projectDir, Map<String, Map<String, String>> iniData) throws IOException {
        Map<String, String> javacSection = iniData.get("javac");
        if (javacSection == null) {
            throw new IllegalStateException("Missing [javac] section in INI file.");
        }

        var rawPath = javacSection.getOrDefault("path", "").trim();
        if (rawPath.isBlank()) {
            throw new IllegalStateException("Missing JDK path in [javac].path.");
        }

        var jdkRoot = BuildConfig.jdkRoot(projectDir, iniData);
        if (BuildConfig.toolExecutable(jdkRoot, "javac").canRead()) {
            return;
        }

        if (IniEnvironment.referencesEnvironment(rawPath)) {
            throw new IllegalStateException("JDK not found at " + jdkRoot.getPath() + ".");
        }

        var downloadUrl = downloadUrl(javacSection);
        if (downloadUrl == null) {
            throw new IllegalStateException(
                    "JDK not found at " + jdkRoot.getPath()
                            + ". Configure [javac].download."
                            + platformKey()
                            + " or set path = $JAVA_HOME.");
        }

        install(projectDir, jdkRoot, downloadUrl);
        if (!BuildConfig.toolExecutable(jdkRoot, "javac").canRead()) {
            throw new IOException("JDK install completed but javac is missing at " + jdkRoot.getPath());
        }
    }

    public static void install(File projectDir, File jdkRoot, String downloadUrl) throws IOException {
        var stagingDir = new File(projectDir, STAGING_DIR);
        ArchiveExtractor.deleteRecursively(stagingDir);
        if (!stagingDir.mkdirs()) {
            throw new IOException("Cannot create " + stagingDir.getPath());
        }

        var archive = new File(stagingDir, archiveFileName(downloadUrl));
        System.out.println("Downloading JDK to " + jdkRoot.getPath() + ":");
        HttpFiles.download(downloadUrl, archive);
        System.out.println("  " + archive.getName());

        var extractedDir = new File(stagingDir, "extracted");
        if (!extractedDir.mkdirs()) {
            throw new IOException("Cannot create " + extractedDir.getPath());
        }
        ArchiveExtractor.extract(archive, extractedDir);

        var discoveredRoot = ArchiveExtractor.findJdkRoot(extractedDir);
        ArchiveExtractor.deleteRecursively(jdkRoot);
        if (!jdkRoot.mkdirs()) {
            throw new IOException("Cannot create " + jdkRoot.getPath());
        }
        ArchiveExtractor.moveDirectoryContents(discoveredRoot, jdkRoot);
        ArchiveExtractor.deleteRecursively(stagingDir);
        System.out.println("JDK installed.");
    }

    private static boolean hasDownloadUrl(Map<String, String> javacSection) {
        for (var entry : javacSection.entrySet()) {
            if (entry.getKey().startsWith("download.") && !entry.getValue().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static String downloadUrl(Map<String, String> javacSection) {
        var key = "download." + platformKey();
        var url = javacSection.getOrDefault(key, "").trim();
        return url.isBlank() ? null : IniEnvironment.expand(url);
    }

    static String platformKey() {
        var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.startsWith("windows")) {
            return "windows";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "macos";
        }
        return "linux";
    }

    private static String archiveFileName(String downloadUrl) {
        try {
            return Dependencies.fileNameFromUrl(downloadUrl);
        } catch (IllegalStateException e) {
            return "jdk-" + platformKey() + ".archive";
        }
    }
}
