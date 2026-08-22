package thelaboflieven.info.build;

import thelaboflieven.info.ProjectContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Subprojects {
    private Subprojects() {
    }

    public static List<Subproject> read(Map<String, Map<String, String>> iniData) {
        Map<String, String> section = iniData.get("subproject");
        if (section == null) {
            return List.of();
        }

        var subprojects = new ArrayList<Subproject>();
        for (var entry : section.entrySet()) {
            var name = entry.getKey().trim();
            var path = entry.getValue().trim();
            if (name.isBlank() || path.isBlank()) {
                continue;
            }
            subprojects.add(new Subproject(name, path));
        }
        return subprojects;
    }

    public static File iniFile(File projectDir, Subproject subproject) {
        var subIni = new File(new File(projectDir, subproject.path()), ProjectContext.DEFAULT_INI_FILE);
        if (!subIni.canRead()) {
            throw new IllegalStateException("Cannot read subproject build.ini: " + subIni.getPath());
        }
        return subIni;
    }

    public static ProjectContext load(File projectDir, Subproject subproject) throws IOException {
        return ProjectContext.load(iniFile(projectDir, subproject).getAbsolutePath());
    }
}
