package thelaboflieven.info.download;

import thelaboflieven.info.ProjectContext;
import thelaboflieven.info.build.Subprojects;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependencyOrchestrator {
    public int install(File iniFile) throws IOException, InterruptedException {
        return install(ProjectContext.load(iniFile.getAbsolutePath()));
    }

    public int install(ProjectContext project) throws IOException, InterruptedException {
        return install(project, new HashSet<>(), true);
    }

    public int installProject(ProjectContext project) throws IOException {
        return installProject(project, false);
    }

    private int install(
            ProjectContext project,
            Set<String> visitedInChain,
            boolean isRoot
    ) throws IOException, InterruptedException {
        return Subprojects.withCycleGuard(project, visitedInChain, () -> {
            int installed = 0;
            var subprojects = Subprojects.read(project.iniData());
            for (var subproject : subprojects) {
                System.out.println(
                        "Installing dependencies for subproject " + subproject.name()
                                + " (" + subproject.path() + ")");
                installed += install(
                        Subprojects.load(project.projectDir(), subproject),
                        visitedInChain,
                        false);
            }
            installed += installProject(project, isRoot && subprojects.isEmpty());
            return installed;
        });
    }

    private int installProject(ProjectContext project, boolean warnWhenEmpty) throws IOException {
        var artifacts = Dependencies.artifacts(project.iniData());
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
            System.out.println("Dependencies from " + project.iniFile().getName() + ":");
            installed = installJars(project.projectDir(), artifacts);
        }
        return installed;
    }

    private int installJars(File projectDir, List<Dependencies.Artifact> artifacts) throws IOException {
        var dependenciesDir = new File(projectDir, Dependencies.DIRECTORY);
        if (!dependenciesDir.exists() && !dependenciesDir.mkdirs()) {
            throw new IOException("Cannot create " + dependenciesDir.getPath());
        }

        int downloaded = 0;
        for (var artifact : artifacts) {
            var target = new File(dependenciesDir, artifact.fileName());
            if (target.isFile()) {
                System.out.println("  " + artifact.fileName() + " (already present)");
                continue;
            }
            HttpFiles.download(artifact.url(), target);
            System.out.println("  " + artifact.fileName());
            downloaded++;
        }
        return downloaded;
    }
}
