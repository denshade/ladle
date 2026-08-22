package thelaboflieven.info;

import thelaboflieven.info.build.BuildCleaner;
import thelaboflieven.info.build.BuildFailedException;
import thelaboflieven.info.build.CompileOrchestrator;
import thelaboflieven.info.build.JarPackager;
import thelaboflieven.info.build.Subprojects;
import thelaboflieven.info.download.DependencyInstaller;
import thelaboflieven.info.download.JdkInstaller;
import thelaboflieven.info.test.TestFailedException;
import thelaboflieven.info.test.TestOrchestrator;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Ladle {
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
            case "release" -> runRelease(args);
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
            new CompileOrchestrator().compile(buildIni);
        } catch (BuildFailedException e) {
            System.err.println(e.getMessage() + ".");
            System.exit(e.exitCode());
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private static void runRelease(String[] args) throws IOException, InterruptedException {
        var buildIni = resolveIniFile("release", args);
        try {
            var project = ProjectContext.load(buildIni.getAbsolutePath());
            new CompileOrchestrator().compile(project);
            new JarPackager().packageRelease(project);
            System.out.println("Release successful.");
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
        try {
            var installed = installDependencies(
                    ProjectContext.load(buildIni.getAbsolutePath()),
                    new HashSet<>(),
                    true);
            if (installed > 0) {
                System.out.println("Dependencies installed.");
            }
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private static int installDependencies(
            ProjectContext project,
            Set<String> visitedInChain,
            boolean isRoot
    ) throws IOException {
        var canonicalPath = project.iniFile().getCanonicalPath();
        if (!visitedInChain.add(canonicalPath)) {
            throw new IllegalStateException("Circular subproject reference: " + project.iniFile().getPath());
        }

        try {
            int installed = 0;
            var subprojects = Subprojects.read(project.iniData());
            for (var subproject : subprojects) {
                System.out.println(
                        "Installing dependencies for subproject " + subproject.name()
                                + " (" + subproject.path() + ")");
                installed += installDependencies(
                        Subprojects.load(project.projectDir(), subproject),
                        visitedInChain,
                        false);
            }
            installed += installProjectDependencies(project, isRoot && subprojects.isEmpty());
            return installed;
        } finally {
            visitedInChain.remove(canonicalPath);
        }
    }

    private static int installProjectDependencies(ProjectContext project, boolean warnWhenEmpty) throws IOException {
        var installer = new DependencyInstaller(project);
        var artifacts = installer.artifacts();
        if (artifacts.isEmpty() && !JdkInstaller.isConfigured(project.iniData())) {
            if (warnWhenEmpty) {
                System.err.println("Warning: no dependencies configured in " + project.iniFile().getName() + ".");
            }
            return 0;
        }

        int installed = 0;
        if (JdkInstaller.isConfigured(project.iniData())) {
            JdkInstaller.ensureInstalled(project.projectDir(), project.iniData());
        }

        if (!artifacts.isEmpty()) {
            System.out.println("Downloading " + artifacts.size() + " dependency file(s) to dependencies/ from "
                    + project.iniFile().getName() + ":");
            installed = installer.install(project.projectDir());
        }
        return installed;
    }

    private static void runTest(String[] args) throws IOException, InterruptedException {
        var buildIni = resolveIniFile("test", args);
        try {
            var tested = new TestOrchestrator().test(buildIni);
            if (tested == 0) {
                return;
            }
            System.out.println("Tests successful.");
        } catch (TestFailedException e) {
            System.err.println(e.getMessage() + ".");
            System.exit(e.exitCode());
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
