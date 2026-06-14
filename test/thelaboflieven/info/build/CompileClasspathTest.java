package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CompileClasspathTest {
    @Test
    void combinesSubprojectsAndImplementationDependencies() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-classpath").toFile();
        var dependenciesDir = new File(projectDir, "dependencies");
        dependenciesDir.mkdirs();
        new File(dependenciesDir, "lib.jar").createNewFile();
        new File(dependenciesDir, "core.jar").createNewFile();

        var iniData = Map.of(
                "subproject",
                Map.of("core", "../core"),
                "dependencies",
                Map.of("lib.jar", "https://example.com/lib.jar")
        );

        var classpath = CompileClasspath.resolve(projectDir, iniData);

        assertEquals("dependencies/core.jar" + File.pathSeparator + "dependencies/lib.jar", classpath);
    }

    @Test
    void includesCompileOnlyDependencies() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-classpath-compileonly").toFile();
        var dependenciesDir = new File(projectDir, "dependencies");
        dependenciesDir.mkdirs();
        new File(dependenciesDir, "lib.jar").createNewFile();
        new File(dependenciesDir, "jspecify-1.0.jar").createNewFile();

        var iniData = Map.of(
                "dependencies",
                Map.of("lib.jar", "https://example.com/lib.jar"),
                "compileonlydependencies",
                Map.of("org.jspecify", "https://example.com/jspecify-1.0.jar")
        );

        var classpath = CompileClasspath.resolve(projectDir, iniData);

        assertEquals(
                "dependencies/lib.jar" + File.pathSeparator + "dependencies/jspecify-1.0.jar",
                classpath
        );
    }

    @Test
    void failsWhenDependencyJarMissing() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-classpath-missing").toFile();
        var iniData = Map.of(
                "dependencies",
                Map.of("missing.jar", "https://example.com/missing.jar")
        );

        assertThrows(IllegalStateException.class, () -> CompileClasspath.resolve(projectDir, iniData));
    }
}
