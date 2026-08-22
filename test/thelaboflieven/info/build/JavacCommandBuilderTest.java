package thelaboflieven.info.build;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavacCommandBuilderTest {
    @Test
    void omitsProcessorPathWhenSectionMissing() throws Exception {
        var projectDir = newProject("ladle-javac-no-processor");
        writeJava(projectDir, "src/example/App.java", """
                package example;
                public class App {}
                """);
        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -encoding UTF-8 -d build/classes

                [sources]
                paths = src
                """);

        var plan = new JavacCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).buildPlan();

        assertFalse(plan.command().contains("-processorpath"));
        assertFalse(plan.command().contains("-processor"));
        assertEquals("", plan.processorPath());
    }

    @Test
    void addsProcessorPathWithoutPuttingJarsOnClasspath() throws Exception {
        var projectDir = newProject("ladle-javac-processorpath");
        writeJava(projectDir, "src/example/App.java", """
                package example;
                public class App {}
                """);
        var dependenciesDir = new File(projectDir, "dependencies");
        dependenciesDir.mkdirs();
        new File(dependenciesDir, "lib.jar").createNewFile();
        new File(dependenciesDir, "auto-service-1.1.1.jar").createNewFile();
        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -encoding UTF-8 -d build/classes

                [sources]
                paths = src

                [dependencies]
                lib.jar = https://example.com/lib.jar

                [annotationprocessor]
                auto-service = https://example.com/auto-service-1.1.1.jar
                """);

        var plan = new JavacCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).buildPlan();

        assertEquals("dependencies/lib.jar", plan.classpath());
        assertEquals("dependencies/auto-service-1.1.1.jar", plan.processorPath());
        assertEquals("dependencies/lib.jar", flagValue(plan.command(), "-cp"));
        assertEquals("dependencies/auto-service-1.1.1.jar", flagValue(plan.command(), "-processorpath"));
        assertFalse(plan.command().contains("-processor"));
        assertFalse(flagValue(plan.command(), "-cp").contains("auto-service"));
    }

    @Test
    void addsNamedProcessorClasses() throws Exception {
        var projectDir = newProject("ladle-javac-processor-class");
        writeJava(projectDir, "src/example/App.java", """
                package example;
                public class App {}
                """);
        var dependenciesDir = new File(projectDir, "dependencies");
        dependenciesDir.mkdirs();
        new File(dependenciesDir, "auto-service-1.1.1.jar").createNewFile();
        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes

                [sources]
                paths = src

                [annotationprocessor]
                auto-service = https://example.com/auto-service-1.1.1.jar
                processor = com.google.auto.service.processor.AutoServiceProcessor
                """);

        var plan = new JavacCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).buildPlan();

        assertEquals(
                "com.google.auto.service.processor.AutoServiceProcessor",
                flagValue(plan.command(), "-processor"));
        assertEquals("dependencies/auto-service-1.1.1.jar", flagValue(plan.command(), "-processorpath"));
    }

    @Test
    void failsWhenSourcesSectionMissing() throws Exception {
        var projectDir = newProject("ladle-javac-no-sources");
        writeIni(projectDir, """
                [javac]
                path = .jdk
                parameters = -d build/classes
                """);

        var thrown = assertThrows(
                IllegalStateException.class,
                () -> new JavacCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).buildPlan());
        assertEquals("Missing [sources] section in INI file.", thrown.getMessage());
    }

    private static File newProject(String prefix) throws Exception {
        var projectDir = Files.createTempDirectory(prefix).toFile();
        var binDir = new File(projectDir, ".jdk/bin");
        binDir.mkdirs();
        new File(binDir, BuildConfig.toolFileName("javac")).createNewFile();
        return projectDir;
    }

    private static void writeIni(File projectDir, String contents) throws Exception {
        Files.writeString(new File(projectDir, "build.ini").toPath(), contents);
    }

    private static void writeJava(File projectDir, String relativePath, String contents) throws Exception {
        var file = new File(projectDir, relativePath);
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), contents);
    }

    private static String flagValue(java.util.List<String> command, String flag) {
        for (int i = 0; i < command.size() - 1; i++) {
            if (command.get(i).equals(flag)) {
                return command.get(i + 1);
            }
        }
        throw new AssertionError("Missing " + flag + " in " + command);
    }
}
