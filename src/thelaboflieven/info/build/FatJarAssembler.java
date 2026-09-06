package thelaboflieven.info.build;

import thelaboflieven.info.ProjectContext;
import thelaboflieven.info.download.ArchiveExtractor;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FatJarAssembler {
    private FatJarAssembler() {
    }

    public static String stagingDirectory(ProjectContext project) {
        return BuildConfig.buildDirectory(project.iniData()) + "/fat-classes";
    }

    public static List<String> assemble(ProjectContext project, File classesDir, File stagingDir) throws IOException {
        var stagingRoot = stagingDir.getCanonicalFile().toPath();
        var classesRoot = classesDir.getCanonicalFile().toPath();
        if (stagingRoot.equals(classesRoot)) {
            throw new IllegalStateException(
                    "Fat JAR staging directory cannot be the classes directory: " + stagingDir.getPath());
        }

        ArchiveExtractor.deleteRecursively(stagingDir);
        if (!stagingDir.mkdirs() && !stagingDir.isDirectory()) {
            throw new IOException("Cannot create " + stagingDir.getPath());
        }

        var unpackedJars = CompileClasspath.runtimeJarPaths(project.projectDir(), project.iniData());
        for (var relativeJar : unpackedJars) {
            unpackJar(new File(project.projectDir(), relativeJar), stagingRoot);
        }
        copyProjectClasses(classesRoot, stagingRoot);
        return unpackedJars;
    }

    static void unpackJar(File jarFile, Path stagingRoot) throws IOException {
        if (!isZip(jarFile)) {
            throw new IllegalStateException("Not a zip/jar: " + jarFile.getPath());
        }

        try (var input = new ZipInputStream(new BufferedInputStream(new FileInputStream(jarFile)))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    input.closeEntry();
                    continue;
                }
                var relative = PathGlobs.normalize(entry.getName());
                if (relative.isBlank() || shouldSkipDependencyEntry(relative)) {
                    input.closeEntry();
                    continue;
                }
                var target = resolveInside(stagingRoot, relative, entry.getName());
                writeEntry(input, target, isServiceDescriptor(relative));
                input.closeEntry();
            }
        }
    }

    static boolean shouldSkipDependencyEntry(String relativePath) {
        var lower = relativePath.toLowerCase(Locale.ROOT);
        if (lower.equals("meta-inf/manifest.mf") || lower.equals("meta-inf/index.list")) {
            return true;
        }
        if (!lower.startsWith("meta-inf/")) {
            return false;
        }
        return lower.endsWith(".sf") || lower.endsWith(".dsa") || lower.endsWith(".rsa") || lower.endsWith(".ec");
    }

    static boolean isServiceDescriptor(String relativePath) {
        return relativePath.startsWith("META-INF/services/") && relativePath.length() > "META-INF/services/".length();
    }

    private static void copyProjectClasses(Path classesRoot, Path stagingRoot) throws IOException {
        try (var stream = Files.walk(classesRoot)) {
            var files = stream.filter(Files::isRegularFile).toList();
            for (var file : files) {
                var relative = PathGlobs.normalize(classesRoot.relativize(file).toString());
                var target = resolveInside(stagingRoot, relative, relative);
                if (isServiceDescriptor(relative) && Files.exists(target)) {
                    mergeServiceFile(Files.readAllBytes(file), target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void writeEntry(ZipInputStream input, Path target, boolean mergeServices) throws IOException {
        var bytes = readAll(input);
        Files.createDirectories(target.getParent());
        if (mergeServices && Files.exists(target)) {
            mergeServiceFile(bytes, target);
            return;
        }
        Files.write(target, bytes);
    }

    private static void mergeServiceFile(byte[] incoming, Path target) throws IOException {
        var existing = Files.readAllBytes(target);
        var merged = new ByteArrayOutputStream(existing.length + incoming.length + 1);
        merged.write(existing);
        if (existing.length > 0 && existing[existing.length - 1] != '\n') {
            merged.write('\n');
        }
        merged.write(incoming);
        Files.write(target, merged.toByteArray());
    }

    private static byte[] readAll(ZipInputStream input) throws IOException {
        var buffer = new ByteArrayOutputStream();
        input.transferTo(buffer);
        return buffer.toByteArray();
    }

    private static Path resolveInside(Path stagingRoot, String relative, String originalName) throws IOException {
        var target = stagingRoot.resolve(relative).normalize();
        if (!target.startsWith(stagingRoot)) {
            throw new IOException("Refusing to extract " + originalName + " outside " + stagingRoot);
        }
        return target;
    }

    private static boolean isZip(File file) throws IOException {
        try (var input = new BufferedInputStream(new FileInputStream(file))) {
            var header = input.readNBytes(2);
            return header.length == 2 && header[0] == 'P' && header[1] == 'K';
        }
    }
}
