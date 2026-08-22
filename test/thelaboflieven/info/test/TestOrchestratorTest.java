package thelaboflieven.info.test;

import org.junit.jupiter.api.Test;
import thelaboflieven.info.CommandsRunner;
import thelaboflieven.info.build.BuildConfig;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestOrchestratorTest {
    @Test
    void failsWhenTestAndSubprojectsMissing() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-no-test").toFile();
        writeIni(projectDir, """
                [javac]
                path = .jdk
                """);

        var thrown = assertThrows(
                IllegalStateException.class,
                () -> new TestOrchestrator().test(new File(projectDir, "build.ini")));
        assertEquals(
                "Missing [test] section in INI file. Omit it only when [subproject] is present.",
                thrown.getMessage());
    }

    @Test
    void skipsWhenAggregatorHasOnlySubprojectsWithoutTests() throws Exception {
        var root = Files.createTempDirectory("ladle-test-aggregator").toFile();
        var child = new File(root, "lib");
        child.mkdirs();

        writeIni(root, """
                [subproject]
                lib = lib
                """);
        writeIni(child, """
                [javac]
                path = .jdk

                [sources]
                paths = src
                """);

        var tested = new TestOrchestrator().test(new File(root, "build.ini"));

        assertEquals(0, tested);
    }

    @Test
    void runsTestsInSubprojectsWhenRootHasNoTestSection() throws Exception {
        var root = Files.createTempDirectory("ladle-test-subproject").toFile();
        var child = new File(root, "lib");
        child.mkdirs();
        writeJdkTools(child);

        writeIni(root, """
                [subproject]
                lib = lib
                """);
        writeIni(child, """
                [javac]
                path = .jdk

                [test]
                sources = test
                classpath = build/classes
                output = build/test-classes
                """);
        writeJava(child, "test/example/LibTest.java", """
                package example;
                public class LibTest {}
                """);

        var commands = new ArrayList<List<String>>();
        var tested = new TestOrchestrator(dir -> new CommandsRunner(dir) {
            @Override
            public int run(List<List<String>> projectCommands) {
                commands.addAll(projectCommands);
                return 0;
            }
        }).test(new File(root, "build.ini"));

        assertEquals(1, tested);
        assertEquals(2, commands.size());
        assertTrue(commands.get(0).stream().anyMatch(argument -> argument.endsWith("LibTest.java")));
        assertTrue(commands.get(1).contains("example.LibTest"));
    }

    private static void writeJdkTools(File projectDir) throws Exception {
        var binDir = new File(projectDir, ".jdk/bin");
        binDir.mkdirs();
        new File(binDir, BuildConfig.toolFileName("javac")).createNewFile();
        new File(binDir, BuildConfig.toolFileName("java")).createNewFile();
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
