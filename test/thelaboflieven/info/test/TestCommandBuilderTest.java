package thelaboflieven.info.test;

import org.junit.jupiter.api.Test;
import thelaboflieven.info.build.BuildConfig;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCommandBuilderTest {
    @Test
    void compilesTestsWithoutFixturesWhenSectionMissing() throws Exception {
        var projectDir = newProject("ladle-test-no-fixtures");
        writeJava(projectDir, "test/example/AppTest.java", """
                package example;
                public class AppTest {}
                """);
        writeIni(projectDir, """
                [javac]
                path = .jdk

                [test]
                sources = test
                classpath = build/classes
                output = build/test-classes
                """);

        var plan = new TestCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).buildPlan();

        assertEquals(2, plan.commands().size());
        assertEquals(1, plan.testClassCount());
        assertFalse(flagValue(plan.commands().get(0), "-cp").contains("test-fixtures-classes"));
        assertFalse(plan.classpath().contains("test-fixtures-classes"));
        assertEquals("build/classes" + File.pathSeparator + "build/test-classes", plan.classpath());
    }

    @Test
    void compilesFixturesBeforeTestsAndPrependsOutputToClasspath() throws Exception {
        var projectDir = newProject("ladle-test-fixtures");
        writeJava(projectDir, "src/testFixtures/example/Helper.java", """
                package example;
                public class Helper {}
                """);
        writeJava(projectDir, "test/example/AppTest.java", """
                package example;
                public class AppTest {}
                """);
        writeIni(projectDir, """
                [javac]
                path = .jdk
                release = 17

                [testdependencies]
                junit.jar = https://example.com/junit.jar

                [compileonlydependencies]
                jspecify.jar = https://example.com/jspecify.jar

                [testfixtures]
                sources = src/testFixtures

                [test]
                sources = test
                classpath = build/classes
                output = build/test-classes
                """);

        var plan = new TestCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).buildPlan();

        assertEquals(3, plan.commands().size());
        var fixtureCompile = plan.commands().get(0);
        var testCompile = plan.commands().get(1);
        var testRun = plan.commands().get(2);

        assertEquals("build/test-fixtures-classes", flagValue(fixtureCompile, "-d"));
        assertTrue(containsPath(fixtureCompile, "src/testFixtures/example/Helper.java"));
        var fixtureClasspath = flagValue(fixtureCompile, "-cp");
        assertTrue(fixtureClasspath.startsWith("build/classes"));
        assertTrue(fixtureClasspath.contains("dependencies/junit.jar"));
        assertTrue(fixtureClasspath.contains("dependencies/jspecify.jar"));

        var testCompileClasspath = flagValue(testCompile, "-cp");
        assertTrue(testCompileClasspath.startsWith("build/test-fixtures-classes" + File.pathSeparator));
        assertTrue(testCompileClasspath.contains("build/classes"));
        assertTrue(testCompileClasspath.contains("dependencies/junit.jar"));
        assertTrue(testCompileClasspath.contains("dependencies/jspecify.jar"));

        var expectedRuntime = String.join(
                String.valueOf(File.pathSeparatorChar),
                List.of(
                        "build/test-fixtures-classes",
                        "build/classes",
                        "dependencies/junit.jar",
                        "build/test-classes"));
        assertEquals(expectedRuntime, plan.classpath());
        assertEquals(expectedRuntime, flagValue(testRun, "-cp"));
        assertFalse(plan.classpath().contains("jspecify.jar"));
    }

    @Test
    void compilesAllFixtureJavaFilesNotOnlyTestClasses() throws Exception {
        var projectDir = newProject("ladle-test-fixture-helpers");
        writeJava(projectDir, "src/testFixtures/example/TestBase.java", """
                package example;
                public class TestBase {}
                """);
        writeJava(projectDir, "src/testFixtures/example/Assertions.java", """
                package example;
                public class Assertions {}
                """);
        writeJava(projectDir, "test/example/AppTest.java", """
                package example;
                public class AppTest {}
                """);
        writeIni(projectDir, """
                [javac]
                path = .jdk

                [testfixtures]
                sources = src/testFixtures
                classpath = build/classes
                output = build/fixtures

                [test]
                sources = test
                """);

        var plan = new TestCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).buildPlan();

        var fixtureCompile = plan.commands().get(0);
        assertEquals("build/fixtures", flagValue(fixtureCompile, "-d"));
        assertTrue(containsPath(fixtureCompile, "src/testFixtures/example/TestBase.java"));
        assertTrue(containsPath(fixtureCompile, "src/testFixtures/example/Assertions.java"));
        assertTrue(flagValue(plan.commands().get(1), "-cp").startsWith("build/fixtures" + File.pathSeparator));
    }

    @Test
    void requiresSourcesWhenTestfixturesSectionIsPresent() throws Exception {
        var projectDir = newProject("ladle-test-fixtures-missing-sources");
        writeJava(projectDir, "test/example/AppTest.java", """
                package example;
                public class AppTest {}
                """);
        writeIni(projectDir, """
                [javac]
                path = .jdk

                [testfixtures]
                output = build/test-fixtures-classes

                [test]
                sources = test
                """);

        var iniPath = new File(projectDir, "build.ini").getAbsolutePath();
        var error = assertThrows(IllegalStateException.class, () -> new TestCommandBuilder(iniPath).buildPlan());
        assertEquals("Missing sources in [testfixtures] section of INI file.", error.getMessage());
    }

    @Test
    void skipsFixtureCompileWhenNoTestsAreFound() throws Exception {
        var projectDir = newProject("ladle-test-fixtures-no-tests");
        writeJava(projectDir, "src/testFixtures/example/Helper.java", """
                package example;
                public class Helper {}
                """);
        new File(projectDir, "test").mkdirs();
        writeIni(projectDir, """
                [javac]
                path = .jdk

                [testfixtures]
                sources = src/testFixtures

                [test]
                sources = test
                """);

        var plan = new TestCommandBuilder(new File(projectDir, "build.ini").getAbsolutePath()).buildPlan();

        assertEquals(0, plan.testClassCount());
        assertEquals(List.of(), plan.commands());
        assertTrue(plan.classpath().startsWith("build/test-fixtures-classes" + File.pathSeparator));
    }

    private static File newProject(String prefix) throws Exception {
        var projectDir = Files.createTempDirectory(prefix).toFile();
        var binDir = new File(projectDir, ".jdk/bin");
        binDir.mkdirs();
        new File(binDir, BuildConfig.toolFileName("javac")).createNewFile();
        new File(binDir, BuildConfig.toolFileName("java")).createNewFile();
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

    private static String flagValue(List<String> command, String flag) {
        for (int i = 0; i < command.size() - 1; i++) {
            if (command.get(i).equals(flag)) {
                return command.get(i + 1);
            }
        }
        throw new AssertionError("Missing " + flag + " in " + command);
    }

    private static boolean containsPath(List<String> command, String relativePath) {
        var suffix = relativePath.replace('/', File.separatorChar);
        return command.stream().anyMatch(argument -> argument.endsWith(suffix));
    }
}
