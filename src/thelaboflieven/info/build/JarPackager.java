package thelaboflieven.info.build;

import thelaboflieven.info.CommandsRunner;
import thelaboflieven.info.CommandLine;
import thelaboflieven.info.ProjectContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JarPackager {
    public void packageRelease(ProjectContext project) throws IOException, InterruptedException {
        var jarBuilder = new JarCommandBuilder(project);
        var outputJar = jarBuilder.releaseOutputJar();
        System.out.println("Packaging " + outputJar.getName() + "...");
        packageJar(project, jarBuilder, outputJar);
    }

    public void packageJar(ProjectContext project, File outputJar) throws IOException, InterruptedException {
        packageJar(project, new JarCommandBuilder(project), outputJar);
    }

    private void packageJar(
            ProjectContext project,
            JarCommandBuilder jarBuilder,
            File outputJar
    ) throws IOException, InterruptedException {
        outputJar.getParentFile().mkdirs();
        var jarPlan = jarBuilder.planFor(outputJar);
        var runner = new CommandsRunner(project.projectDir());
        System.out.println("  jar: " + CommandLine.format(jarPlan.command()));
        var exitCode = runner.run(List.of(jarPlan.command()));
        if (exitCode != 0) {
            throw new BuildFailedException(exitCode);
        }
        if (!outputJar.canRead()) {
            throw new IllegalStateException("Failed to create " + outputJar.getPath());
        }
        System.out.println("Created " + outputJar.getPath());
    }
}
