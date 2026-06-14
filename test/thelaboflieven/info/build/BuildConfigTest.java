package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BuildConfigTest {
    @Test
    void toolFileNameUsesExeSuffixOnWindows() {
        var osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.startsWith("windows")) {
            assertEquals("javac.exe", BuildConfig.toolFileName("javac"));
            assertEquals("java.exe", BuildConfig.toolFileName("java"));
            assertEquals("jar.exe", BuildConfig.toolFileName("jar"));
        } else {
            assertEquals("javac", BuildConfig.toolFileName("javac"));
            assertEquals("java", BuildConfig.toolFileName("java"));
            assertEquals("jar", BuildConfig.toolFileName("jar"));
        }
    }

    @Test
    void resolvesJdkToolInBinDirectory() throws Exception {
        var jdkRoot = Files.createTempDirectory("ladle-jdk").toFile();
        var binDir = new File(jdkRoot, "bin");
        binDir.mkdirs();
        var tool = new File(binDir, BuildConfig.toolFileName("javac"));
        tool.createNewFile();

        var resolved = BuildConfig.toolExecutable(jdkRoot, "javac");

        assertEquals(tool.getCanonicalPath(), resolved.getPath());
    }

    @Test
    void failsWhenJdkToolMissing() throws Exception {
        var jdkRoot = Files.createTempDirectory("ladle-jdk-missing").toFile();
        new File(jdkRoot, "bin").mkdirs();

        assertThrows(IllegalStateException.class, () -> BuildConfig.toolExecutable(jdkRoot, "javac"));
    }
}
