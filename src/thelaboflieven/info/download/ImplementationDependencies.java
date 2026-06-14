package thelaboflieven.info.download;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ImplementationDependencies {
    private ImplementationDependencies() {
    }

    public static List<String> localPaths(Map<String, Map<String, String>> iniData) {
        return localPathsFromSection(iniData.get("dependencies"));
    }

    static List<String> localPathsFromSection(Map<String, String> dependencies) {
        if (dependencies == null) {
            return List.of();
        }

        var paths = new ArrayList<String>();
        for (var entry : dependencies.entrySet()) {
            var name = entry.getKey().trim();
            var url = entry.getValue().trim();
            if (name.isBlank() || url.isBlank()) {
                continue;
            }
            paths.add(TestDependencies.localPath(name, url));
        }
        return paths;
    }
}
