package thelaboflieven.info.build;

import thelaboflieven.info.inifile.IniFileReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ResourceCopier {
    private final File projectDir;
    private final Map<String, Map<String, String>> iniData;

    public ResourceCopier(String iniFilePath) throws IOException {
        var iniFile = new File(iniFilePath);
        projectDir = iniFile.getParentFile();
        iniData = new IniFileReader().parseIniFile(iniFilePath);
    }

    public ResourceCopyPlan copyResources() throws IOException {
        Map<String, String> resourcesSection = iniData.get("resources");
        if (resourcesSection == null || resourcesSection.isEmpty()) {
            return new ResourceCopyPlan(0);
        }

        var classesDir = new File(projectDir, BuildConfig.classesDirectory(iniData));
        classesDir.mkdirs();

        var copiedCount = 0;
        var paths = resourcesSection.get("paths");
        if (paths != null && !paths.isBlank()) {
            for (var pathEntry : paths.split(",")) {
                var sourceRoot = new File(projectDir, pathEntry.trim());
                if (!sourceRoot.isDirectory()) {
                    continue;
                }
                copiedCount += copyTree(sourceRoot.toPath(), classesDir.toPath(), sourceRoot.toPath());
            }
        }

        for (var entry : resourcesSection.entrySet()) {
            if ("paths".equals(entry.getKey())) {
                continue;
            }
            var source = new File(projectDir, entry.getKey().trim());
            if (!source.exists()) {
                throw new IllegalStateException("Missing resource source: " + source.getPath());
            }
            var destination = new File(classesDir, entry.getValue().trim());
            copiedCount += copyEntry(source.toPath(), destination.toPath());
        }

        return new ResourceCopyPlan(copiedCount);
    }

    private int copyTree(Path sourceRoot, Path classesDir, Path current) throws IOException {
        var copiedCount = 0;
        try (var stream = Files.walk(current)) {
            var paths = stream.filter(Files::isRegularFile).toList();
            for (var file : paths) {
                if (file.toString().endsWith(".java")) {
                    continue;
                }
                var relative = sourceRoot.relativize(file);
                var target = classesDir.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.copy(file, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                copiedCount++;
            }
        }
        return copiedCount;
    }

    private int copyEntry(Path source, Path destination) throws IOException {
        if (Files.isDirectory(source)) {
            return copyTree(source, destination, source);
        }
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return 1;
    }
}
