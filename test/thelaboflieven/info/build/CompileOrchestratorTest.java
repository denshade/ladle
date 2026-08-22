package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompileOrchestratorTest {
    @Test
    void failsWhenSourcesAndSubprojectsMissing() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-no-sources").toFile();
        writeIni(projectDir, """
                [javac]
                path = .jdk
                """);

        var thrown = assertThrows(
                IllegalStateException.class,
                () -> new CompileOrchestrator().compile(new File(projectDir, "build.ini")));
        assertEquals(
                "Missing [sources] section in INI file. Omit it only when [subproject] is present.",
                thrown.getMessage());
    }

    @Test
    @EnabledIf("javacAvailable")
    void skipsCompileWhenOnlySubprojectsArePresent() throws Exception {
        var root = Files.createTempDirectory("ladle-aggregator").toFile();
        var child = new File(root, "lib");
        child.mkdirs();

        writeIni(root, """
                [subproject]
                lib = lib
                """);
        writeIni(child, """
                [javac]
                path = %s
                parameters = -encoding UTF-8 -d build/classes

                [sources]
                paths = src
                """.formatted(jdkRoot().replace('\\', '/')));
        writeJava(child, "src/example/Lib.java", """
                package example;
                public class Lib {}
                """);

        new CompileOrchestrator().compile(new File(root, "build.ini"));

        assertTrue(new File(child, "build/classes/example/Lib.class").isFile());
        assertTrue(new File(root, "dependencies/lib.jar").isFile());
        assertFalse(new File(root, "build/javac.args").exists());
        assertFalse(new File(root, "build/classes").exists());
    }

    static boolean javacAvailable() {
        return new File(jdkRoot(), "bin" + File.separator + BuildConfig.toolFileName("javac")).canRead()
                && new File(jdkRoot(), "bin" + File.separator + BuildConfig.toolFileName("jar")).canRead();
    }

    private static String jdkRoot() {
        return System.getProperty("java.home");
    }

    private static void writeIni(File projectDir, String contents) throws Exception {
        Files.writeString(new File(projectDir, "build.ini").toPath(), contents);
    }

    private static void writeJava(File projectDir, String relativePath, String contents) throws Exception {
        var file = new File(projectDir, relativePath);
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), contents);
    }
}
