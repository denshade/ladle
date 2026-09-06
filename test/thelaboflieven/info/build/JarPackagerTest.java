package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import thelaboflieven.info.ProjectContext;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JarPackagerTest {
    @Test
    @EnabledIf("jarAvailable")
    void packageReleaseWritesFatJarWithRuntimeClasses() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-packager-fat").toFile();
        var classesDir = new File(projectDir, "build/classes/example");
        classesDir.mkdirs();
        Files.writeString(new File(classesDir, "App.class").toPath(), "app");

        var dependenciesDir = new File(projectDir, "dependencies");
        dependenciesDir.mkdirs();
        try (var zip = new ZipOutputStream(new FileOutputStream(new File(dependenciesDir, "lib.jar")))) {
            zip.putNextEntry(new ZipEntry("example/Lib.class"));
            zip.write("lib".getBytes());
            zip.closeEntry();
        }

        writeIni(projectDir, """
                [javac]
                path = %s
                parameters = -d build/classes

                [sources]
                paths = src

                [dependencies]
                lib.jar = https://example.com/lib.jar

                [jar]
                name = app
                main-class = example.App
                fat = true
                """.formatted(jdkRoot().replace('\\', '/')));

        var project = ProjectContext.load(new File(projectDir, "build.ini").getAbsolutePath());
        new JarPackager().packageRelease(project);

        var outputJar = new File(projectDir, "build/app.jar");
        try (var zip = new ZipFile(outputJar)) {
            assertEquals("app", new String(zip.getInputStream(zip.getEntry("example/App.class")).readAllBytes()));
            assertEquals("lib", new String(zip.getInputStream(zip.getEntry("example/Lib.class")).readAllBytes()));
            assertNotNull(zip.getEntry("META-INF/MANIFEST.MF"));
            assertNull(zip.getEntry("META-INF/INDEX.LIST"));
        }
    }

    static boolean jarAvailable() {
        return new File(jdkRoot(), "bin" + File.separator + BuildConfig.toolFileName("jar")).canRead();
    }

    private static String jdkRoot() {
        return System.getProperty("java.home");
    }

    private static void writeIni(File projectDir, String contents) throws Exception {
        Files.writeString(new File(projectDir, "build.ini").toPath(), contents);
    }
}
