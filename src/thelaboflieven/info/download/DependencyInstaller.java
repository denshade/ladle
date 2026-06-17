package thelaboflieven.info.download;

import thelaboflieven.info.ProjectContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DependencyInstaller {
    private final ProjectContext project;

    public DependencyInstaller(String iniFilePath) throws IOException {
        this(ProjectContext.load(iniFilePath));
    }

    public DependencyInstaller(ProjectContext project) {
        this.project = project;
    }

    public List<DependencyArtifact> artifacts() {
        return Dependencies.artifacts(project.iniData());
    }

    public int install(File projectDir) throws IOException {
        var artifacts = artifacts();
        if (artifacts.isEmpty()) {
            return 0;
        }

        var dependenciesDir = new File(projectDir, DependencyPaths.DIRECTORY);
        if (!dependenciesDir.exists() && !dependenciesDir.mkdirs()) {
            throw new IOException("Cannot create " + dependenciesDir.getPath());
        }

        for (var artifact : artifacts) {
            var target = new File(dependenciesDir, artifact.fileName());
            HttpFiles.download(artifact.url(), target);
            System.out.println("  " + artifact.fileName());
        }
        return artifacts.size();
    }
}
