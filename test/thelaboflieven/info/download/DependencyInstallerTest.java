package thelaboflieven.info.download;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DependencyInstallerTest {
    @Test
    void skipsDownloadWhenJarAlreadyPresent() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-deps-present").toFile();
        writeIni(projectDir, """
                [dependencies]
                lib.jar = https://127.0.0.1:1/lib.jar
                """);
        var jar = new File(new File(projectDir, DependencyPaths.DIRECTORY), "lib.jar");
        jar.getParentFile().mkdirs();
        Files.writeString(jar.toPath(), "existing");

        var downloaded = new DependencyInstaller(new File(projectDir, "build.ini").getAbsolutePath())
                .install(projectDir);

        assertEquals(0, downloaded);
        assertEquals("existing", Files.readString(jar.toPath()));
    }

    @Test
    void downloadsWhenJarMissing() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-deps-missing").toFile();
        writeIni(projectDir, """
                [dependencies]
                lib.jar = http://127.0.0.1:1/lib.jar
                """);

        var installer = new DependencyInstaller(new File(projectDir, "build.ini").getAbsolutePath());
        assertThrows(IOException.class, () -> installer.install(projectDir));
        assertFalse(new File(projectDir, "dependencies/lib.jar").isFile());
    }

    private static void writeIni(File projectDir, String contents) throws Exception {
        Files.writeString(new File(projectDir, "build.ini").toPath(), contents);
    }
}
