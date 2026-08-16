package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JarCommandBuilderTest {
    @Test
    void releaseOutputJarUsesJarSectionAndBuildDirectory() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-jar").toFile();
        writeIni(projectDir, """
                [javac]
                path = .

                [sources]
                paths = src

                [build]
                directory = build

                [jar]
                name = example
                """);

        var outputJar = new JarCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).releaseOutputJar();

        assertEquals(new File(projectDir, "build/example.jar").getAbsolutePath(), outputJar.getPath());
    }

    @Test
    void releaseOutputJarUsesJarDirectoryOverride() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-jar-dir").toFile();
        writeIni(projectDir, """
                [javac]
                path = .

                [sources]
                paths = src

                [jar]
                name = example
                directory = dist
                """);

        var outputJar = new JarCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).releaseOutputJar();

        assertEquals(new File(projectDir, "dist/example.jar").getAbsolutePath(), outputJar.getPath());
    }

    @Test
    void releaseOutputJarDefaultsNameToProjectDirectory() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-jar-default-name").toFile();
        writeIni(projectDir, """
                [javac]
                path = .

                [sources]
                paths = src

                [jar]
                """);

        var outputJar = new JarCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).releaseOutputJar();

        assertEquals(
                new File(projectDir, "build/" + projectDir.getName() + ".jar").getAbsolutePath(),
                outputJar.getPath());
    }

    @Test
    void planForUsesManifestFileWhenConfigured() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-jar-manifest").toFile();
        var classesDir = new File(projectDir, "build/classes");
        classesDir.mkdirs();
        var manifestDir = new File(projectDir, "manifest");
        manifestDir.mkdirs();
        Files.writeString(new File(manifestDir, "MANIFEST.MF").toPath(), "Main-Class: example.Main\n");
        createFakeJdk(projectDir);

        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [jar]
                name = example
                manifest = manifest/MANIFEST.MF
                """);

        var builder = new JarCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath());
        var plan = builder.planFor(new File(projectDir, "build/example.jar"));

        assertEquals(
                List.of(
                        new File(projectDir, ".jdk/bin/" + BuildConfig.toolFileName("jar")).getPath(),
                        "cfm",
                        new File(projectDir, "build/example.jar").getAbsolutePath(),
                        "manifest/MANIFEST.MF",
                        "-C",
                        "build/classes",
                        "."),
                plan.command());
    }

    @Test
    void planForGeneratesManifestFromMainClass() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-jar-main-class").toFile();
        new File(projectDir, "build/classes").mkdirs();
        createFakeJdk(projectDir);

        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [jar]
                name = example
                main-class = example.Main
                """);

        var builder = new JarCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath());
        builder.planFor(new File(projectDir, "build/example.jar"));

        var generatedManifest = new File(projectDir, "build/MANIFEST.MF");
        assertEquals("Main-Class: example.Main\n", Files.readString(generatedManifest.toPath()));
    }

    @Test
    void planForPackagesAllClassesWhenNoIncludeExclude() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-jar-all").toFile();
        var classesDir = new File(projectDir, "build/classes/example");
        classesDir.mkdirs();
        Files.writeString(new File(classesDir, "Keep.class").toPath(), "keep");
        createFakeJdk(projectDir);

        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [jar]
                name = example
                """);

        var plan = new JarCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath())
                .planFor(new File(projectDir, "build/example.jar"));

        assertEquals(
                List.of(
                        new File(projectDir, ".jdk/bin/" + BuildConfig.toolFileName("jar")).getPath(),
                        "cf",
                        new File(projectDir, "build/example.jar").getAbsolutePath(),
                        "-C",
                        "build/classes",
                        "."),
                plan.command());
    }

    @Test
    void planForListsFilesMatchingIncludeAndExclude() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-jar-filter").toFile();
        writeClassTree(projectDir);
        createFakeJdk(projectDir);

        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [jar]
                name = example
                include = **/*.class, **/*.properties
                exclude = module-info.class, **/Drop.class
                """);

        var plan = new JarCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath())
                .planFor(new File(projectDir, "build/example.jar"));

        assertEquals(
                List.of(
                        new File(projectDir, ".jdk/bin/" + BuildConfig.toolFileName("jar")).getPath(),
                        "cf",
                        new File(projectDir, "build/example.jar").getAbsolutePath(),
                        "-C",
                        "build/classes",
                        "example/Keep.class",
                        "example/config.properties"),
                plan.command());
    }

    @Test
    void planForOmitsIncludeExcludeFromGeneratedManifest() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-jar-filter-manifest").toFile();
        writeClassTree(projectDir);
        createFakeJdk(projectDir);

        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [jar]
                name = example
                main-class = example.Main
                include = **/*.class
                exclude = module-info.class
                implementation-title = Example
                """);

        new JarCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath())
                .planFor(new File(projectDir, "build/example.jar"));

        assertEquals(
                "Main-Class: example.Main\n\nImplementation-Title: Example\n",
                Files.readString(new File(projectDir, "build/MANIFEST.MF").toPath()));
    }

    @Test
    void planForFailsWhenNoFilesMatchIncludeExclude() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-jar-filter-none").toFile();
        writeClassTree(projectDir);
        createFakeJdk(projectDir);

        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [jar]
                name = example
                include = **/*.html
                """);

        var iniPath = new File(projectDir, "build.ini").getAbsolutePath();
        var outputJar = new File(projectDir, "build/example.jar");
        var error = assertThrows(
                IllegalStateException.class,
                () -> new JarCommandBuilder(iniPath).planFor(outputJar));
        assertEquals("No files matched [jar] include/exclude patterns.", error.getMessage());
    }

    @Test
    void releaseOutputJarRequiresJarSection() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-jar-missing").toFile();
        writeIni(projectDir, """
                [javac]
                path = .

                [sources]
                paths = src
                """);

        var iniPath = new File(projectDir, "build.ini").getAbsolutePath();
        assertThrows(IllegalStateException.class, () -> new JarCommandBuilder(iniPath).releaseOutputJar());
    }

    private static void writeIni(File projectDir, String contents) throws Exception {
        Files.writeString(new File(projectDir, "build.ini").toPath(), contents);
    }

    private static void writeClassTree(File projectDir) throws Exception {
        var exampleDir = new File(projectDir, "build/classes/example");
        exampleDir.mkdirs();
        Files.writeString(new File(exampleDir, "Keep.class").toPath(), "keep");
        Files.writeString(new File(exampleDir, "Drop.class").toPath(), "drop");
        Files.writeString(new File(exampleDir, "config.properties").toPath(), "k=v");
        Files.writeString(new File(projectDir, "build/classes/module-info.class").toPath(), "module");
        var servicesDir = new File(projectDir, "build/classes/META-INF/services");
        servicesDir.mkdirs();
        Files.writeString(new File(servicesDir, "example.Api").toPath(), "example.Keep");
    }

    private static void createFakeJdk(File projectDir) throws Exception {
        var binDir = new File(projectDir, ".jdk/bin");
        binDir.mkdirs();
        new File(binDir, BuildConfig.toolFileName("javac")).createNewFile();
        new File(binDir, BuildConfig.toolFileName("jar")).createNewFile();
    }
}
