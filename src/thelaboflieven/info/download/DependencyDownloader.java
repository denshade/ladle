package thelaboflieven.info.download;

import thelaboflieven.info.inifile.IniFileReader;

import java.io.IOException;
import java.util.Map;

public class DependencyDownloader {
    private final Map<String, Map<String, String>> iniData;

    public DependencyDownloader(String iniFilePath) throws IOException {
        iniData = new IniFileReader().parseIniFile(iniFilePath);
    }

    public void download() {

    }
}
