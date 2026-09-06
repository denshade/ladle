package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;
import thelaboflieven.info.ProjectContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FatJarAssemblerTest {
    @Test
    void unpacksRuntimeJarsThenProjectClasses() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-fat").toFile();
        var classesDir = new File(projectDir, "build/classes");
        var exampleDir = new File(classesDir, "example");
        exampleDir.mkdirs();
        Files.writeString(new File(exampleDir, "App.class").toPath(), "app");
        Files.writeString(new File(exampleDir, "Shared.class").toPath(), "from-project");

        var dependenciesDir = new File(projectDir, "dependencies");
        dependenciesDir.mkdirs();
        writeJar(new File(dependenciesDir, "lib.jar"), "example/Lib.class", "lib", "example/Shared.class", "from-lib");
        writeJar(new File(dependenciesDir, "core.jar"), "example/Core.class", "core");
        writeJar(new File(dependenciesDir, "jspecify-1.0.jar"), "org/jspecify/Nullable.class", "ann");
        writeJar(new File(dependenciesDir, "junit.jar"), "org/junit/Test.class", "test");
        writeJar(new File(dependenciesDir, "auto-service.jar"), "com/google/Auto.class", "proc");

        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [subproject]
                core = ../core

                [dependencies]
                lib.jar = https://example.com/lib.jar

                [compileonlydependencies]
                org.jspecify = https://example.com/jspecify-1.0.jar

                [testdependencies]
                junit.jar = https://example.com/junit.jar

                [annotationprocessor]
                auto-service = https://example.com/auto-service.jar

                [jar]
                name = app
                fat = true
                """);

        var stagingDir = new File(projectDir, "build/fat-classes");
        var unpacked = FatJarAssembler.assemble(
                ProjectContext.load(new File(projectDir, "build.ini").getAbsolutePath()),
                classesDir,
                stagingDir);

        assertEquals(java.util.List.of("dependencies/core.jar", "dependencies/lib.jar"), unpacked);
        assertEquals("app", Files.readString(new File(stagingDir, "example/App.class").toPath()));
        assertEquals("lib", Files.readString(new File(stagingDir, "example/Lib.class").toPath()));
        assertEquals("core", Files.readString(new File(stagingDir, "example/Core.class").toPath()));
        assertEquals("from-project", Files.readString(new File(stagingDir, "example/Shared.class").toPath()));
        assertFalse(new File(stagingDir, "org/jspecify/Nullable.class").exists());
        assertFalse(new File(stagingDir, "org/junit/Test.class").exists());
        assertFalse(new File(stagingDir, "com/google/Auto.class").exists());
    }

    @Test
    void skipsDependencyManifestAndSignatureFilesAndMergesServices() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-fat-services").toFile();
        var classesDir = new File(projectDir, "build/classes");
        var servicesDir = new File(classesDir, "META-INF/services");
        servicesDir.mkdirs();
        Files.writeString(new File(servicesDir, "example.Api").toPath(), "example.App");
        Files.writeString(new File(classesDir, "META-INF/MANIFEST.MF").toPath(), "Main-Class: example.App\n");

        var dependenciesDir = new File(projectDir, "dependencies");
        dependenciesDir.mkdirs();
        try (var zip = new ZipOutputStream(new FileOutputStream(new File(dependenciesDir, "lib.jar")))) {
            writeZipFile(zip, "META-INF/MANIFEST.MF", "Main-Class: example.Lib\n");
            writeZipFile(zip, "META-INF/LIB.SF", "signature");
            writeZipFile(zip, "META-INF/LIB.DSA", "dsa");
            writeZipFile(zip, "META-INF/INDEX.LIST", "index");
            writeZipFile(zip, "META-INF/services/example.Api", "example.Lib");
            writeZipFile(zip, "example/Lib.class", "lib");
        }

        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [dependencies]
                lib.jar = https://example.com/lib.jar

                [jar]
                name = app
                fat = true
                """);

        var stagingDir = new File(projectDir, "build/fat-classes");
        FatJarAssembler.assemble(
                ProjectContext.load(new File(projectDir, "build.ini").getAbsolutePath()),
                classesDir,
                stagingDir);

        assertEquals("lib", Files.readString(new File(stagingDir, "example/Lib.class").toPath()));
        assertEquals("Main-Class: example.App\n", Files.readString(new File(stagingDir, "META-INF/MANIFEST.MF").toPath()));
        assertFalse(new File(stagingDir, "META-INF/LIB.SF").exists());
        assertFalse(new File(stagingDir, "META-INF/LIB.DSA").exists());
        assertFalse(new File(stagingDir, "META-INF/INDEX.LIST").exists());
        assertEquals(
                "example.Lib\nexample.App",
                Files.readString(new File(stagingDir, "META-INF/services/example.Api").toPath()));
    }

    @Test
    void rejectsZipEntriesOutsideStagingDirectory() throws Exception {
        var stagingDir = Files.createTempDirectory("ladle-fat-zipslip").toFile();
        var jarFile = File.createTempFile("evil", ".jar");
        try (var zip = new ZipOutputStream(new FileOutputStream(jarFile))) {
            writeZipFile(zip, "../escape.class", "nope");
        }

        var error = assertThrows(
                IOException.class,
                () -> FatJarAssembler.unpackJar(jarFile, stagingDir.getCanonicalFile().toPath()));
        assertTrue(error.getMessage().contains("Refusing to extract"));
    }

    @Test
    void failsWhenDependencyIsNotAZip() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-fat-notzip").toFile();
        var classesDir = new File(projectDir, "build/classes");
        classesDir.mkdirs();
        var dependenciesDir = new File(projectDir, "dependencies");
        dependenciesDir.mkdirs();
        Files.writeString(new File(dependenciesDir, "lib.jar").toPath(), "not a jar");

        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [dependencies]
                lib.jar = https://example.com/lib.jar

                [jar]
                fat = true
                """);

        var error = assertThrows(
                IllegalStateException.class,
                () -> FatJarAssembler.assemble(
                        ProjectContext.load(new File(projectDir, "build.ini").getAbsolutePath()),
                        classesDir,
                        new File(projectDir, "build/fat-classes")));
        assertTrue(error.getMessage().startsWith("Not a zip/jar:"));
    }

    @Test
    void failsWhenStagingDirectoryIsTheClassesDirectory() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-fat-overlap").toFile();
        var classesDir = new File(projectDir, "build/classes");
        classesDir.mkdirs();
        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [jar]
                fat = true
                """);

        var error = assertThrows(
                IllegalStateException.class,
                () -> FatJarAssembler.assemble(
                        ProjectContext.load(new File(projectDir, "build.ini").getAbsolutePath()),
                        classesDir,
                        classesDir));
        assertTrue(error.getMessage().startsWith("Fat JAR staging directory cannot be the classes directory"));
    }

    private static void writeIni(File projectDir, String contents) throws Exception {
        Files.writeString(new File(projectDir, "build.ini").toPath(), contents);
    }

    private static void writeJar(File jarFile, String... pathAndContents) throws Exception {
        try (var zip = new ZipOutputStream(new FileOutputStream(jarFile))) {
            for (int i = 0; i < pathAndContents.length; i += 2) {
                writeZipFile(zip, pathAndContents[i], pathAndContents[i + 1]);
            }
        }
    }

    private static void writeZipFile(ZipOutputStream zip, String path, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes());
        zip.closeEntry();
    }
}
