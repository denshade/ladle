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
        for (var impl: dependencies.get("implementation").split(",")) {
            var implParts = impl.split("/");
            var lastPart = implParts[implParts.length - 1];
            StringBuilder s = new StringBuilder();
            commands.add(s.append("wget ").append(impl).append("  -OutFile ").append(lastPart).toString());
        }
        return commands;
    }
}
