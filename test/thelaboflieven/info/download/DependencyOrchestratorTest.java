package thelaboflieven.info.download;

import org.junit.jupiter.api.Test;
import thelaboflieven.info.ProjectContext;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DependencyOrchestratorTest {
    @Test
    void installProjectSkipsJarsAlreadyOnDisk() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-orch-present").toFile();
        writeIni(projectDir, """
                [dependencies]
                lib.jar = https://127.0.0.1:1/lib.jar
                """);
        var jar = new File(new File(projectDir, DependencyPaths.DIRECTORY), "lib.jar");
        jar.getParentFile().mkdirs();
        Files.writeString(jar.toPath(), "existing");

        var installed = new DependencyOrchestrator().installProject(
                ProjectContext.load(new File(projectDir, "build.ini").getAbsolutePath()));

        assertEquals(0, installed);
        assertEquals("existing", Files.readString(jar.toPath()));
    }

    @Test
    void installWalksSubprojectsFirst() throws Exception {
        var root = Files.createTempDirectory("ladle-orch-tree").toFile();
        var child = new File(root, "lib");
        child.mkdirs();
        writeIni(root, """
                [subproject]
                lib = lib
                """);
        writeIni(child, """
                [dependencies]
                lib.jar = https://127.0.0.1:1/lib.jar
                """);
        var jar = new File(new File(child, DependencyPaths.DIRECTORY), "lib.jar");
        jar.getParentFile().mkdirs();
        Files.writeString(jar.toPath(), "child");

        var installed = new DependencyOrchestrator().install(new File(root, "build.ini"));

        assertEquals(0, installed);
        assertTrue(jar.isFile());
    }

    private static void writeIni(File projectDir, String contents) throws Exception {
        Files.writeString(new File(projectDir, "build.ini").toPath(), contents);
    }
}
