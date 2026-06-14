package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceCopierTest {
    @Test
    void copiesResourcePathsIntoClassesDirectory() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-resources").toFile();
        var resourcesDir = new File(projectDir, "src/main/resources/com/example");
        resourcesDir.mkdirs();
        Files.writeString(new File(resourcesDir, "config.properties").toPath(), "key=value");
        Files.writeString(new File(resourcesDir, "Ignored.java").toPath(), "class Ignored {}");

        writeIni(projectDir, """
                [javac]
                path = .
                parameters = -d build/classes

                [sources]
                paths = src

                [resources]
                paths = src/main/resources
                """);

        var plan = new ResourceCopier(new File(projectDir, "build.ini").getAbsolutePath()).copyResources();

        assertEquals(1, plan.fileCount());
        assertTrue(new File(projectDir, "build/classes/com/example/config.properties").isFile());
        assertTrue(!new File(projectDir, "build/classes/com/example/Ignored.java").exists());
    }

    @Test
    void copiesExplicitResourceRules() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-resources-rules").toFile();
        var generatedDir = new File(projectDir, "build/generated");
        generatedDir.mkdirs();
        Files.writeString(new File(generatedDir, "inject-MockMethodDispatcher.raw").toPath(), "raw");

        writeIni(projectDir, """
                [javac]
                path = .
                parameters = -d build/classes

                [sources]
                paths = src

                [resources]
                build/generated/inject-MockMethodDispatcher.raw = inject-MockMethodDispatcher.raw
                """);

        var plan = new ResourceCopier(new File(projectDir, "build.ini").getAbsolutePath()).copyResources();

        assertEquals(1, plan.fileCount());
        assertTrue(new File(projectDir, "build/classes/inject-MockMethodDispatcher.raw").isFile());
    }

    @Test
    void skipsMissingResourcePaths() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-resources-missing-path").toFile();

        writeIni(projectDir, """
                [javac]
                path = .
                parameters = -d build/classes

                [sources]
                paths = src

                [resources]
                paths = does/not/exist
                """);

        var plan = new ResourceCopier(new File(projectDir, "build.ini").getAbsolutePath()).copyResources();

        assertEquals(0, plan.fileCount());
    }

    @Test
    void failsWhenExplicitResourceSourceMissing() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-resources-missing-rule").toFile();

        writeIni(projectDir, """
                [javac]
                path = .
                parameters = -d build/classes

                [sources]
                paths = src

                [resources]
                missing/file.raw = file.raw
                """);

        var iniPath = new File(projectDir, "build.ini").getAbsolutePath();
        assertThrows(IllegalStateException.class, () -> new ResourceCopier(iniPath).copyResources());
    }

    @Test
    void returnsZeroWhenResourcesSectionMissing() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-resources-none").toFile();

        writeIni(projectDir, """
                [javac]
                path = .
                parameters = -d build/classes

                [sources]
                paths = src
                """);

        var plan = new ResourceCopier(new File(projectDir, "build.ini").getAbsolutePath()).copyResources();

        assertEquals(0, plan.fileCount());
    }

    private static void writeIni(File projectDir, String contents) throws Exception {
        Files.writeString(new File(projectDir, "build.ini").toPath(), contents);
    }
}
