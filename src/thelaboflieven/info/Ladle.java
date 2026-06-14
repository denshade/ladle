package thelaboflieven.info;

import thelaboflieven.info.build.BuildFailedException;
import thelaboflieven.info.build.BuildOrchestrator;
import thelaboflieven.info.build.BuildCleaner;
import thelaboflieven.info.download.DependencyInstaller;
import thelaboflieven.info.download.JdkInstaller;
import thelaboflieven.info.inifile.IniFileReader;
import thelaboflieven.info.test.TestCommandBuilder;
import thelaboflieven.info.test.TestPlan;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Ladle {
    private static final String DEFAULT_INI = "build.ini";

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 0) {
            printWelcome();
            return;
        }

        if (args.length == 1 && args[0].equals("--help")) {
            printHelp();
            return;
        }

        String command = args[0];
        switch (command) {
            case "build" -> runBuild(args);
            case "dependency" -> runDependency(args);
            case "test" -> runTest(args);
            case "clear" -> runClear(args);
            default -> {
                System.err.println("Unknown command: " + command);
                System.err.println("Run ladle --help for usage.");
                System.exit(1);
            }
        }
    }

    private static void runBuild(String[] args) throws IOException, InterruptedException {
        var buildIni = resolveIniFile("build", args);
        try {
            new BuildOrchestrator().build(buildIni);
        } catch (BuildFailedException e) {
            System.err.println(e.getMessage() + ".");
            System.exit(e.exitCode());
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private static void runDependency(String[] args) throws IOException {
        var buildIni = resolveIniFile("dependency", args);
        var iniData = new IniFileReader().parseIniFile(buildIni.getAbsolutePath());
        var projectDir = buildIni.getParentFile();

        var installer = new DependencyInstaller(buildIni.getAbsolutePath());
        var artifacts = installer.artifacts();
        if (artifacts.isEmpty() && !JdkInstaller.isConfigured(iniData)) {
            System.err.println("Warning: no dependencies configured in " + buildIni.getName() + ".");
            return;
        }

        if (JdkInstaller.isConfigured(iniData)) {
            JdkInstaller.ensureInstalled(projectDir, iniData);
        }

        if (!artifacts.isEmpty()) {
            System.out.println("Downloading " + artifacts.size() + " dependency file(s) to dependencies/ from " + buildIni.getName() + ":");
            installer.install(projectDir);
            System.out.println("Dependencies installed.");
        }
    }

    private static void runTest(String[] args) throws IOException, InterruptedException {
        var buildIni = resolveIniFile("test", args);
        try {
            var builder = new TestCommandBuilder(buildIni.getAbsolutePath());
            var plan = builder.buildPlan();
            if (plan.testClassCount() == 0) {
                System.err.println("Warning: no test classes found in " + buildIni.getName() + ".");
                return;
            }
            printTestPlan(buildIni, plan);
            var commandRunner = new CommandsRunner(buildIni.getParentFile());
            var exitCode = commandRunner.run(plan.commands());
            if (exitCode != 0) {
                System.err.println("Tests failed with exit code " + exitCode + ".");
                System.exit(exitCode);
            }
            System.out.println("Tests successful.");
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private static void runClear(String[] args) throws IOException {
        var buildIni = resolveIniFile("clear", args);
        var cleaner = new BuildCleaner(buildIni.getAbsolutePath());
        var cleared = cleaner.clear(buildIni.getParentFile());
        if (!cleared) {
            System.err.println("Warning: build directory '" + cleaner.buildDirectory() + "' does not exist.");
            return;
        }
        System.out.println("Cleared " + cleaner.buildDirectory() + "/");
    }

    private static void printTestPlan(File buildIni, TestPlan plan) {
        System.out.println("Testing from " + buildIni.getName());
        System.out.println("Running " + plan.testClassCount() + " test class(es) with JUnit 5");
        System.out.println("  java: " + plan.javaPath());
        System.out.println("  classpath: " + plan.classpath());
        System.out.println("  runner: " + plan.runner());
        for (int i = 0; i < plan.commands().size(); i++) {
            System.out.println("  command " + (i + 1) + ": " + plan.commands().get(i));
        }
    }

    private static File resolveIniFile(String command, String[] args) {
        if (args.length > 2) {
            System.err.println("Too many arguments.");
            System.err.println("Usage: ladle " + command + " [<ini-file>]");
            System.exit(2);
        }

        String iniPath = args.length == 2 ? args[1] : DEFAULT_INI;
        var buildIni = new File(iniPath);
        if (!buildIni.canRead()) {
            if (args.length == 1) {
                System.err.println("Cannot read " + DEFAULT_INI + " in the current directory.");
            } else {
                System.err.println("Cannot read " + iniPath);
            }
            System.err.println("Usage: ladle " + command + " [<ini-file>]");
            System.exit(2);
        }
        return buildIni;
    }

    private static void printWelcome() {
        System.out.println("thelaboflieven.info.Ladle version 0.2");
        System.out.println("Welcome to thelaboflieven.info.Ladle 0.2");
        System.out.println("To see a list of command-line options, run ladle --help\n");
    }

    private static void printHelp() {
        System.out.println("thelaboflieven.info.Ladle version 0.2");
        System.out.println("Usage:");
        System.out.println("  ladle build [<ini-file>]       Compile Java sources (default: build.ini)");
        System.out.println("  ladle dependency [<ini-file>] Download JDK and dependencies (default: build.ini)");
        System.out.println("  ladle test [<ini-file>]        Run unit tests (default: build.ini)");
        System.out.println("  ladle clear [<ini-file>]       Delete the build directory (default: build.ini)");
        System.out.println("  ladle --help                   Show this help message");
    }
}
