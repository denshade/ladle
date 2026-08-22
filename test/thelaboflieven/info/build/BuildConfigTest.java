package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void javacVersionFlagsUsesRelease() {
        var flags = BuildConfig.javacVersionFlags(Map.of("release", "21"));

        assertEquals(List.of("--release", "21"), flags);
    }

    @Test
    void javacVersionFlagsUsesSourceAndTarget() {
        var flags = BuildConfig.javacVersionFlags(Map.of("source", "17", "target", "17"));

        assertEquals(List.of("-source", "17", "-target", "17"), flags);
    }

    @Test
    void javacVersionFlagsRejectsReleaseWithSourceOrTarget() {
        assertThrows(
                IllegalStateException.class,
                () -> BuildConfig.javacVersionFlags(Map.of("release", "21", "source", "17")));
    }

    @Test
    void javacParameterSummaryCombinesVersionFlagsAndParameters() {
        var summary = BuildConfig.javacParameterSummary(Map.of(
                "release", "21",
                "parameters", "-encoding UTF-8 -d build/classes"));

        assertEquals("--release 21 -encoding UTF-8 -d build/classes", summary);
    }

    @Test
    void hasSourcesWhenSectionPresent() {
        assertTrue(BuildConfig.hasSources(Map.of("sources", Map.of("paths", "src"))));
        assertFalse(BuildConfig.hasSources(Map.of("javac", Map.of("path", ".jdk"))));
    }
}
