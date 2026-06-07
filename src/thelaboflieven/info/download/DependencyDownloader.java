package thelaboflieven.info.download;

import thelaboflieven.info.inifile.IniFileReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DependencyDownloader {
    private final Map<String, Map<String, String>> iniData;

    public DependencyDownloader(String iniFilePath) throws IOException {
        iniData = new IniFileReader().parseIniFile(iniFilePath);
    }

    public List<String> download() {
        var commands = new ArrayList<String>();
        Map<String, String> dependencies = iniData.get("dependencies");
        if (dependencies != null) {
            addDownloadsFromList(commands, dependencies.get("implementation"));
        }

        Map<String, String> testDependencies = iniData.get("testdependencies");
        if (testDependencies != null) {
            addDownloadsFromPairs(commands, testDependencies);
        }
        return commands;
    }

    private void addDownloadsFromList(List<String> commands, String dependencyList) {
        if (dependencyList == null || dependencyList.isBlank()) {
            return;
        }

        for (var url : dependencyList.split(",")) {
            url = url.trim();
            if (url.isBlank()) {
                continue;
            }
            var urlParts = url.split("/");
            var fileName = urlParts[urlParts.length - 1];
            commands.add("powershell.exe wget " + url + "  -OutFile " + fileName);
        }
    }

    private void addDownloadsFromPairs(List<String> commands, Map<String, String> pairs) {
        for (var entry : pairs.entrySet()) {
            var name = entry.getKey().trim();
            var url = entry.getValue().trim();
            if (name.isBlank() || url.isBlank()) {
                continue;
            }
            commands.add("powershell.exe wget " + url + "  -OutFile " + name);
        }
    }
}
