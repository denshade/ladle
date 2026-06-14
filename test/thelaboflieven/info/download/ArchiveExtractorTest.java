package thelaboflieven.info.download;

import org.junit.jupiter.api.Test;
import thelaboflieven.info.build.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArchiveExtractorTest {
    @Test
    void extractsZipAndFindsJdkRoot() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-archive").toFile();
        var archive = new File(projectDir, "jdk.zip");
        var extracted = new File(projectDir, "extracted");

        try (var zip = new ZipOutputStream(new FileOutputStream(archive))) {
            writeDirectory(zip, "jdk-21/bin/");
            var toolName = BuildConfig.toolFileName("javac");
            writeFile(zip, "jdk-21/bin/" + toolName, new byte[] {0});
        }

        ArchiveExtractor.extract(archive, extracted);
        var jdkRoot = ArchiveExtractor.findJdkRoot(extracted);

        assertEquals("jdk-21", jdkRoot.getName());
        assertTrue(new File(jdkRoot, "bin/" + BuildConfig.toolFileName("javac")).isFile());
    }

    private void writeDirectory(ZipOutputStream zip, String path) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.closeEntry();
    }

    private void writeFile(ZipOutputStream zip, String path, byte[] content) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content);
        zip.closeEntry();
    }
}
