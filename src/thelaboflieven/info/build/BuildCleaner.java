package thelaboflieven.info.build;

import thelaboflieven.info.ProjectContext;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class BuildCleaner {
    private final String buildDirectory;

    public BuildCleaner(String iniFilePath) throws IOException {
        this(ProjectContext.load(iniFilePath));
    }

    public BuildCleaner(ProjectContext project) {
        buildDirectory = BuildConfig.buildDirectory(project.iniData());
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
