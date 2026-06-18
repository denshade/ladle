package thelaboflieven.info;

import thelaboflieven.info.inifile.IniFileReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class ProjectContext {
    public static final String DEFAULT_INI_FILE = "build.ini";

    private final File iniFile;
    private final File projectDir;
    private final Map<String, Map<String, String>> iniData;

    private ProjectContext(File iniFile, File projectDir, Map<String, Map<String, String>> iniData) {
        this.iniFile = iniFile;
        this.projectDir = projectDir;
        this.iniData = iniData;
    }

    public static ProjectContext load(String iniFilePath) throws IOException {
        var iniFile = new File(iniFilePath);
        var iniData = new IniFileReader().parseIniFile(iniFilePath);
        return new ProjectContext(iniFile, iniFile.getParentFile(), iniData);
    }

    public File iniFile() {
        return iniFile;
    }

    public File projectDir() {
        return projectDir;
    }

    public Map<String, Map<String, String>> iniData() {
        return iniData;
    }
}
