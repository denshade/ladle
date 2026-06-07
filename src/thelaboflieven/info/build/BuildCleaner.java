package thelaboflieven.info.build;

import thelaboflieven.info.inifile.IniFileReader;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

public class BuildCleaner {
    private static final String DEFAULT_BUILD_DIRECTORY = "build";

    private final String buildDirectory;

    public BuildCleaner(String iniFilePath) throws IOException {
        var iniData = new IniFileReader().parseIniFile(iniFilePath);
        Map<String, String> buildSection = iniData.get("build");
        if (buildSection != null && !buildSection.getOrDefault("directory", "").isBlank()) {
            buildDirectory = buildSection.get("directory").trim();
        } else {
            buildDirectory = DEFAULT_BUILD_DIRECTORY;
        }
    }

    public String buildDirectory() {
        return buildDirectory;
    }

    public boolean clear(File projectDirectory) throws IOException {
        var buildPath = new File(projectDirectory, buildDirectory).toPath().toAbsolutePath().normalize();
        if (!Files.exists(buildPath)) {
            return false;
        }

        try (var paths = Files.walk(buildPath)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return true;
    }
}
