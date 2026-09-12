package thelaboflieven.info.download;

import thelaboflieven.info.ProjectContext;
import thelaboflieven.info.build.Subprojects;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DependencyOrchestrator {
    public int install(File iniFile) throws IOException {
        return install(ProjectContext.load(iniFile.getAbsolutePath()));
    }

    public int install(ProjectContext project) throws IOException {
        return install(project, new HashSet<>(), true);
    }

    public int installProject(ProjectContext project) throws IOException {
        return installProject(project, false);
    }

    private int install(
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
                installed += install(
                        Subprojects.load(project.projectDir(), subproject),
                        visitedInChain,
                        false);
            }
            installed += installProject(project, isRoot && subprojects.isEmpty());
            return installed;
        } finally {
            visitedInChain.remove(canonicalPath);
        }
    }

    private int installProject(ProjectContext project, boolean warnWhenEmpty) throws IOException {
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
            System.out.println("Dependencies from " + project.iniFile().getName() + ":");
            installed = installer.install(project.projectDir());
        }
        return installed;
    }
}
