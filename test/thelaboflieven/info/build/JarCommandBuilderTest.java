package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

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
                new File(projectDir, ".jdk/bin/" + BuildConfig.toolFileName("jar")).getPath()
                        + " cfm "
                        + new File(projectDir, "build/example.jar").getAbsolutePath()
                        + " manifest/MANIFEST.MF -C build/classes .",
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

    private static void createFakeJdk(File projectDir) throws Exception {
        var binDir = new File(projectDir, ".jdk/bin");
        binDir.mkdirs();
        new File(binDir, BuildConfig.toolFileName("javac")).createNewFile();
        new File(binDir, BuildConfig.toolFileName("jar")).createNewFile();
    }
}
