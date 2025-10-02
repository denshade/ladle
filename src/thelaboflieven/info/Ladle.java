package thelaboflieven.info;

import thelaboflieven.info.build.JavacCommandBuilder;
import thelaboflieven.info.download.DependencyDownloader;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Ladle {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("thelaboflieven.info.Ladle version 0.1");
        if (args.length == 0){
            System.out.println("Welcome to thelaboflieven.info.Ladle 0.1");
            System.out.println("To see a list of command-line options, run ladlew --help\n");
        }

        if (args.length == 1 && args[0].equals("--help")) {
            System.out.println("Based on your ini file ladle compiles specific directories");
            System.exit(1);
        }
        if (args.length == 2 && args[0].equals("build")) {
            var buildIni = readFileOrQuit(args);
            var builder = new JavacCommandBuilder(buildIni.getAbsolutePath());
            var runner = builder.buildCommand();
            var commandRunner = new CommandsRunner(buildIni.getParentFile());
            commandRunner.run(List.of(runner));
        }
        if (args.length == 2 && args[0].equals("dependency")) {
            var buildIni = readFileOrQuit(args);
            var builder = new DependencyDownloader(buildIni.getAbsolutePath());
            var downloaders = builder.download();
            var commandRunner = new CommandsRunner(buildIni.getParentFile());
            commandRunner.run(downloaders);
        }
    }

    private static File readFileOrQuit(String[] args) {
        var buildIni = new File(args[1]);
        if(!buildIni.canRead()) {
            System.err.println("Cannot read " + args[1]);
            System.exit(2);
        }
        return buildIni;
    }

}