package thelaboflieven.info.download;

import java.util.List;
import java.util.Map;

public final class CompileOnlyDependencies {
    private CompileOnlyDependencies() {
    }

    public static List<String> localPaths(Map<String, Map<String, String>> iniData) {
        return ImplementationDependencies.localPathsFromSection(iniData.get("compileonlydependencies"));
    }
}
