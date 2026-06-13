package thelaboflieven.info.download;

import thelaboflieven.info.inifile.IniFileReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DependencyInstaller {
    private final Map<String, Map<String, String>> iniData;

    public DependencyInstaller(String iniFilePath) throws IOException {
        iniData = new IniFileReader().parseIniFile(iniFilePath);
    }

    public List<DependencyArtifact> artifacts() {
        var artifacts = new ArrayList<DependencyArtifact>();

        Map<String, String> dependencies = iniData.get("dependencies");
        if (dependencies != null) {
            addFromList(artifacts, dependencies.get("implementation"));
        }

        Map<String, String> testDependencies = iniData.get("testdependencies");
        if (testDependencies != null) {
            addFromPairs(artifacts, testDependencies);
        }
        return artifacts;
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
            download(artifact.url(), target);
            System.out.println("  " + artifact.fileName());
        }
        return artifacts.size();
    }

    private void addFromList(List<DependencyArtifact> artifacts, String dependencyList) {
        if (dependencyList == null || dependencyList.isBlank()) {
            return;
        }

        for (var url : dependencyList.split(",")) {
            url = url.trim();
            if (url.isBlank()) {
                continue;
            }
            var urlParts = url.split("/");
            artifacts.add(new DependencyArtifact(url, urlParts[urlParts.length - 1]));
        }
    }

    private void addFromPairs(List<DependencyArtifact> artifacts, Map<String, String> pairs) {
        for (var entry : pairs.entrySet()) {
            var name = entry.getKey().trim();
            var url = entry.getValue().trim();
            if (name.isBlank() || url.isBlank()) {
                continue;
            }
            artifacts.add(new DependencyArtifact(url, TestDependencies.fileName(name, url)));
        }
    }

    private void download(String url, File target) throws IOException {
        var connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "ladle");
        connection.connect();

        var status = connection.getResponseCode();
        if (status >= 400) {
            throw new IOException("HTTP " + status + " downloading " + url);
        }

        try (InputStream input = connection.getInputStream()) {
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }
    }
}
