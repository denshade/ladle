package thelaboflieven.info;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public final class ProjectPaths {
    private ProjectPaths() {
    }

    public record Located(Path root, Path file) {
    }

    public static String relativeTo(File projectDir, File file) throws IOException {
        var projectPath = projectDir.getCanonicalFile().toPath();
        var filePath = file.getCanonicalFile().toPath();
        if (filePath.startsWith(projectPath)) {
            return projectPath.relativize(filePath).toString().replace('\\', '/');
        }
        return file.getPath();
    }

    public static List<String> commaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        var entries = new ArrayList<String>();
        for (String entry : value.split(",")) {
            entry = entry.trim();
            if (!entry.isBlank()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public static List<Located> collect(
            File projectDir,
            String csvPaths,
            String suffix,
            String missingPathMessage
    ) throws IOException {
        var files = new ArrayList<Located>();
        for (var sourceRoot : commaSeparated(csvPaths)) {
            var root = new File(projectDir, sourceRoot);
            if (!root.isDirectory()) {
                throw new IllegalStateException(missingPathMessage + root.getPath());
            }
            var rootPath = root.toPath().toAbsolutePath().normalize();
            try (var stream = Files.walk(rootPath)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(suffix))
                        .forEach(path -> files.add(new Located(rootPath, path)));
            }
        }
        return files;
    }

    public static void deleteRecursively(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        Files.walkFileTree(
                file.toPath(),
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        Files.delete(path);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }
}
