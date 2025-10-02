package thelaboflieven.info;

import thelaboflieven.info.build.JavacCommandBuilder;
import thelaboflieven.info.download.DependencyDownloader;

import java.io.File;
import java.io.IOException;

public class Ladle {
    public static void main(String[] args) throws IOException {
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
            var buildIni = new File(args[1]);
            if(!buildIni.canRead()) {
                System.err.println("Cannot read " + args[1]);
                System.exit(2);
            }
            var builder = new JavacCommandBuilder(buildIni.getAbsolutePath());
            var runner = builder.buildCommand();
            System.out.println(runner);
        }
        if (args.length == 2 && args[0].equals("dependency")) {
            var buildIni = new File(args[1]);
            if(!buildIni.canRead()) {
                System.err.println("Cannot read " + args[1]);
                System.exit(2);
            }
            var builder = new DependencyDownloader(buildIni.getAbsolutePath());
            var downloader = builder.download();
            System.out.println(downloader);
        }
        //compile.ini
        /*
        [javac]
        javac path
        [source]
        path=directory 1,directory 2

        parameters to javac.
         */
        //Support for compile
        //Support for download
        //Support for jar
        //Support for test
        //Load data from build.gradle.kts
    }

}