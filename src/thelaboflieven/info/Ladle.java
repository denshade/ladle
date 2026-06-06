package thelaboflieven.info;

import thelaboflieven.info.build.BuildPlan;
import thelaboflieven.info.build.JavacCommandBuilder;
import thelaboflieven.info.download.DependencyDownloader;

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
            var builder = new JavacCommandBuilder(buildIni.getAbsolutePath());
            var plan = builder.buildPlan();
            printBuildPlan(buildIni, plan);
            var commandRunner = new CommandsRunner(buildIni.getParentFile());
            var exitCode = commandRunner.run(List.of(plan.command()));
            if (exitCode != 0) {
                System.err.println("Build failed with exit code " + exitCode + ".");
                System.exit(exitCode);
            }
            System.out.println("Build successful.");
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private static void printBuildPlan(File buildIni, BuildPlan plan) {
        System.out.println("Building from " + buildIni.getName());
        System.out.println("Compiling " + plan.sourceFileCount() + " Java source file(s)");
        System.out.println("  javac: " + plan.javacPath());
        if (!plan.parameters().isBlank()) {
            System.out.println("  parameters: " + plan.parameters());
        }
        System.out.println("Running javac...");
    }

    private static void runDependency(String[] args) throws IOException, InterruptedException {
        var buildIni = resolveIniFile("dependency", args);
        try {
            var builder = new DependencyDownloader(buildIni.getAbsolutePath());
            var downloaders = builder.download();
            var commandRunner = new CommandsRunner(buildIni.getParentFile());
            var exitCode = commandRunner.run(downloaders);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
        } catch (IllegalStateException | NullPointerException e) {
            System.err.println(missingDependenciesMessage(e));
            System.exit(2);
        }
    }

    private static String missingDependenciesMessage(Exception e) {
        if (e instanceof NullPointerException) {
            return "Missing [dependencies] section or implementation key in INI file.";
        }
        return e.getMessage();
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
        System.out.println("thelaboflieven.info.Ladle version 0.1");
        System.out.println("Welcome to thelaboflieven.info.Ladle 0.1");
        System.out.println("To see a list of command-line options, run ladle --help\n");
    }

    private static void printHelp() {
        System.out.println("thelaboflieven.info.Ladle version 0.1");
        System.out.println("Usage:");
        System.out.println("  ladle build [<ini-file>]       Compile Java sources (default: build.ini)");
        System.out.println("  ladle dependency [<ini-file>] Download dependencies (default: build.ini)");
        System.out.println("  ladle --help                   Show this help message");
    }
}
