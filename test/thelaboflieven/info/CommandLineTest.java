package thelaboflieven.info;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandLineTest {
    @Test
    void splitParametersRespectsQuotedValues() {
        var tokens = CommandLine.splitParameters("-encoding UTF-8 -d \"build/classes out\"");

        assertEquals(List.of("-encoding", "UTF-8", "-d", "build/classes out"), tokens);
    }

    @Test
    void formatQuotesArgumentsWithSpaces() {
        assertEquals(
                "\"C:\\Program Files\\Java\\jdk-26\\bin\\javac.exe\" -d build/classes",
                CommandLine.format(List.of("C:\\Program Files\\Java\\jdk-26\\bin\\javac.exe", "-d", "build/classes")));
    }

    @Test
    void javacCommandUsesArgfileWhenCommandLineIsTooLong() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-argfile").toFile();
        var arguments = new java.util.ArrayList<String>();
        arguments.add("-cp");
        arguments.add("build/classes");
        for (int i = 0; i < 200; i++) {
            arguments.add(new File(projectDir, "src/File" + i + ".java").getAbsolutePath());
        }

        var command = CommandLine.javacCommand(
                "C:\\Program Files\\Java\\jdk-26\\bin\\javac.exe",
                arguments,
                projectDir,
                "build/javac.args");

        assertEquals(2, command.size());
        assertEquals("C:\\Program Files\\Java\\jdk-26\\bin\\javac.exe", command.get(0));
        assertTrue(command.get(1).startsWith("@build/javac.args"));
        assertTrue(new File(projectDir, "build/javac.args").isFile());
    }

    @Test
    void javacCommandKeepsInlineArgumentsWhenShortEnough() throws Exception {
        var projectDir = Files.createTempDirectory("ladle-inline").toFile();
        var arguments = List.of("-d", "build/classes", new File(projectDir, "src/Main.java").getAbsolutePath());

        var command = CommandLine.javacCommand(
                "javac",
                arguments,
                projectDir,
                "build/javac.args");

        assertEquals(4, command.size());
        assertEquals("javac", command.get(0));
        assertEquals("-d", command.get(1));
        assertFalse(new File(projectDir, "build/javac.args").exists());
    }
}
