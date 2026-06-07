package thelaboflieven.info.build;

import thelaboflieven.info.inifile.IniFileReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class JarCommandBuilder {
    private final File projectDir;
    private final Map<String, Map<String, String>> iniData;

    public JarCommandBuilder(String iniFilePath) throws IOException {
        var iniFile = new File(iniFilePath);
        projectDir = iniFile.getParentFile();
        iniData = new IniFileReader().parseIniFile(iniFilePath);
    }

    public JarPlan planFor(File outputJar) {
        String classesDir = BuildConfig.classesDirectory(iniData);
        var classesPath = new File(projectDir, classesDir);
        var jarTool = BuildConfig.jarExecutable(projectDir, iniData);
        if (!classesPath.isDirectory()) {
            throw new IllegalStateException("Missing compiled classes directory: " + classesPath.getPath());
        }

        var outputPath = outputJar.getAbsolutePath();
        var command = jarTool.getPath() + " cf " + outputPath + " -C " + classesDir + " .";
        return new JarPlan(command, outputPath, classesPath.getPath());
    }
}
