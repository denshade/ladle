package thelaboflieven.info;

import thelaboflieven.info.build.BuildFailedException;
import thelaboflieven.info.build.BuildOrchestrator;
import thelaboflieven.info.build.BuildCleaner;
import thelaboflieven.info.download.DependencyInstaller;
import thelaboflieven.info.download.JdkInstaller;
import thelaboflieven.info.test.TestCommandBuilder;
import thelaboflieven.info.test.TestPlan;

import java.io.File;
import java.io.IOException;

public class Ladle {
    @FunctionalInterface
    private interface OrchestratorAction {
        void run(BuildOrchestrator orchestrator, File iniFile) throws IOException, InterruptedException;
    }

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
            case "build" -> runOrchestrator("build", args, BuildOrchestrator::build);
            case "release" -> runOrchestrator("release", args, BuildOrchestrator::release);
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

    private static void runOrchestrator(String command, String[] args, OrchestratorAction action)
            throws IOException, InterruptedException {
        var buildIni = resolveIniFile(command, args);
        try {
            action.run(new BuildOrchestrator(), buildIni);
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
        var project = ProjectContext.load(buildIni.getAbsolutePath());

        var installer = new DependencyInstaller(project);
        var artifacts = installer.artifacts();
        if (artifacts.isEmpty() && !JdkInstaller.isConfigured(project.iniData())) {
            System.err.println("Warning: no dependencies configured in " + buildIni.getName() + ".");
            return;
        }

        if (JdkInstaller.isConfigured(project.iniData())) {
            JdkInstaller.ensureInstalled(project.projectDir(), project.iniData());
        }

        if (!artifacts.isEmpty()) {
            System.out.println("Downloading " + artifacts.size() + " dependency file(s) to dependencies/ from "
                    + buildIni.getName() + ":");
            installer.install(project.projectDir());
            System.out.println("Dependencies installed.");
        }
    }

    private static void runTest(String[] args) throws IOException, InterruptedException {
        var buildIni = resolveIniFile("test", args);
        try {
            var project = ProjectContext.load(buildIni.getAbsolutePath());
            var builder = new TestCommandBuilder(project);
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
            System.out.println("  command " + (i + 1) + ": " + CommandLine.format(plan.commands().get(i)));
        }
    }

    private static File resolveIniFile(String command, String[] args) {
        if (args.length > 2) {
            System.err.println("Too many arguments.");
            System.err.println("Usage: ladle " + command + " [<ini-file>]");
            System.exit(2);
        }

        String iniPath = args.length == 2 ? args[1] : ProjectContext.DEFAULT_INI_FILE;
        var buildIni = new File(iniPath);
        if (!buildIni.canRead()) {
            if (args.length == 1) {
                System.err.println("Cannot read " + ProjectContext.DEFAULT_INI_FILE + " in the current directory.");
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
        System.out.println("  ladle release [<ini-file>]     Compile and package a JAR (default: build.ini)");
        System.out.println("  ladle dependency [<ini-file>] Download JDK and dependencies (default: build.ini)");
        System.out.println("  ladle test [<ini-file>]        Run unit tests (default: build.ini)");
        System.out.println("  ladle clear [<ini-file>]       Delete the build directory (default: build.ini)");
        System.out.println("  ladle --help                   Show this help message");
    }
}
