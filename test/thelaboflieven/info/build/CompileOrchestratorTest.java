package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;

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
        assertFalse(new File(root, "build/lib.jar").exists());
    }

    @Test
    @EnabledIf("javacAvailable")
    void packagesRootJarWhenJarSectionIsPresent() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-build-jar").toFile();
        writeIni(projectDir, """
                [javac]
                path = %s

                [sources]
                paths = src

                [jar]
                name = app
                """.formatted(jdkRoot().replace('\\', '/')));
        writeJava(projectDir, "src/example/App.java", """
                package example;
                public class App {}
                """);

        new CompileOrchestrator().compile(new File(projectDir, "build.ini"));

        assertTrue(new File(projectDir, "build/classes/example/App.class").isFile());
        assertTrue(new File(projectDir, "build/app.jar").isFile());
    }

    @Test
    @EnabledIf("javacAvailable")
    void doesNotPackageRootJarWhenJarSectionIsMissing() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-build-no-jar").toFile();
        writeIni(projectDir, """
                [javac]
                path = %s

                [sources]
                paths = src
                """.formatted(jdkRoot().replace('\\', '/')));
        writeJava(projectDir, "src/example/App.java", """
                package example;
                public class App {}
                """);

        new CompileOrchestrator().compile(new File(projectDir, "build.ini"));

        assertTrue(new File(projectDir, "build/classes/example/App.class").isFile());
        assertFalse(new File(projectDir, "build/" + projectDir.getName() + ".jar").exists());
    }

    @Test
    @EnabledIf("javacAvailable")
    void downloadsMissingDependencyJarsBeforeCompile() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-build-deps").toFile();
        var jarBytes = dummyJarBytes();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/lib.jar", exchange -> {
            exchange.sendResponseHeaders(200, jarBytes.length);
            try (var body = exchange.getResponseBody()) {
                body.write(jarBytes);
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        try {
            var url = "http://127.0.0.1:" + server.getAddress().getPort() + "/lib.jar";
            writeIni(projectDir, """
                    [javac]
                    path = %s

                    [sources]
                    paths = src

                    [dependencies]
                    lib.jar = %s
                    """.formatted(jdkRoot().replace('\\', '/'), url));
            writeJava(projectDir, "src/example/App.java", """
                    package example;
                    public class App {}
                    """);

            assertFalse(new File(projectDir, "dependencies/lib.jar").exists());
            new CompileOrchestrator().compile(new File(projectDir, "build.ini"));

            assertTrue(new File(projectDir, "dependencies/lib.jar").isFile());
            assertTrue(new File(projectDir, "build/classes/example/App.class").isFile());
        } finally {
            server.stop(0);
        }
    }

    private static byte[] dummyJarBytes() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(bytes)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("META-INF/MANIFEST.MF"));
            zip.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
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
