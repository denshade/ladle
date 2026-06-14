package thelaboflieven.info.download;

import thelaboflieven.info.build.BuildConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ArchiveExtractor {
    private ArchiveExtractor() {
    }

    public static void extract(File archive, File destination) throws IOException {
        switch (detectFormat(archive)) {
            case ZIP -> extractZip(archive, destination);
            case TAR_GZ -> extractTarGz(archive, destination);
            default -> throw new IOException("Unsupported archive format: " + archive.getName());
        }
    }

    private enum ArchiveFormat {
        ZIP,
        TAR_GZ,
        UNKNOWN
    }

    static ArchiveFormat detectFormat(File archive) throws IOException {
        try (var input = new BufferedInputStream(new FileInputStream(archive))) {
            var header = input.readNBytes(2);
            if (header.length >= 2 && header[0] == 'P' && header[1] == 'K') {
                return ArchiveFormat.ZIP;
            }
            if (header.length >= 2 && (header[0] & 0xFF) == 0x1F && (header[1] & 0xFF) == 0x8B) {
                return ArchiveFormat.TAR_GZ;
            }
        }
        var name = archive.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".zip")) {
            return ArchiveFormat.ZIP;
        }
        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            return ArchiveFormat.TAR_GZ;
        }
        return ArchiveFormat.UNKNOWN;
    }

    public static File findJdkRoot(File extractedDir) throws IOException {
        if (containsJavac(extractedDir)) {
            return extractedDir;
        }

        var children = extractedDir.listFiles(File::isDirectory);
        if (children == null) {
            throw new IOException("No JDK root found in " + extractedDir.getPath());
        }

        File candidate = null;
        for (var child : children) {
            if (!containsJavac(child)) {
                continue;
            }
            if (candidate != null) {
                throw new IOException("Multiple JDK roots found in " + extractedDir.getPath());
            }
            candidate = child;
        }

        if (candidate == null) {
            throw new IOException("No JDK root found in " + extractedDir.getPath());
        }
        return candidate;
    }

    public static void moveDirectoryContents(File source, File target) throws IOException {
        if (!source.isDirectory()) {
            throw new IOException("Not a directory: " + source.getPath());
        }
        if (target.exists()) {
            deleteRecursively(target);
        }
        if (!target.mkdirs()) {
            throw new IOException("Cannot create " + target.getPath());
        }

        var entries = source.listFiles();
        if (entries == null) {
            return;
        }
        for (var entry : entries) {
            Files.move(entry.toPath(), new File(target, entry.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
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

    static void extractZip(File archive, File destination) throws IOException {
        if (!destination.exists() && !destination.mkdirs()) {
            throw new IOException("Cannot create " + destination.getPath());
        }

        try (var input = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                var target = new File(destination, entry.getName());
                if (entry.isDirectory()) {
                    if (!target.mkdirs()) {
                        throw new IOException("Cannot create " + target.getPath());
                    }
                } else {
                    var parent = target.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Cannot create " + parent.getPath());
                    }
                    Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                input.closeEntry();
            }
        }
    }

    static void extractTarGz(File archive, File destination) throws IOException {
        if (!destination.exists() && !destination.mkdirs()) {
            throw new IOException("Cannot create " + destination.getPath());
        }

        try (InputStream input = new GZIPInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            readTar(input, destination);
        }
    }

    private static void readTar(InputStream input, File destination) throws IOException {
        while (true) {
            var header = input.readNBytes(512);
            if (header.length < 512) {
                break;
            }
            if (isTarEnd(header)) {
                break;
            }

            var name = readTarString(header, 0, 100);
            if (name.isBlank()) {
                break;
            }

            var size = readTarOctal(header, 124, 12);
            var type = (char) header[156];
            var target = new File(destination, name);
            switch (type) {
                case '0', '\0' -> {
                    var parent = target.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Cannot create " + parent.getPath());
                    }
                    copyTarEntry(input, target, size);
                }
                case '5' -> {
                    if (!target.mkdirs()) {
                        throw new IOException("Cannot create " + target.getPath());
                    }
                    skipTarPadding(input, size);
                }
                default -> skipTarPadding(input, size);
            }
        }
    }

    private static boolean isTarEnd(byte[] header) {
        for (var value : header) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private static String readTarString(byte[] header, int offset, int length) {
        var end = offset;
        while (end < offset + length && header[end] != 0) {
            end++;
        }
        return new String(header, offset, end - offset);
    }

    private static long readTarOctal(byte[] header, int offset, int length) {
        var text = readTarString(header, offset, length).trim();
        if (text.isEmpty()) {
            return 0;
        }
        return Long.parseLong(text, 8);
    }

    private static void copyTarEntry(InputStream input, File target, long size) throws IOException {
        try (var output = Files.newOutputStream(target.toPath())) {
            var remaining = size;
            var buffer = new byte[8192];
            while (remaining > 0) {
                var read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    throw new IOException("Unexpected end of archive while writing " + target.getPath());
                }
                output.write(buffer, 0, read);
                remaining -= read;
            }
        }
        skipTarPadding(input, size);
    }

    private static void skipTarPadding(InputStream input, long size) throws IOException {
        var padding = (512 - (size % 512)) % 512;
        if (padding > 0) {
            input.skipNBytes(padding);
        }
    }

    private static boolean containsJavac(File directory) {
        var javac = new File(directory, "bin" + File.separator + BuildConfig.toolFileName("javac"));
        return javac.canRead();
    }
}
