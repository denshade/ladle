package thelaboflieven.info.build;

import java.util.List;

public record BuildPlan(
        List<String> command,
        int sourceFileCount,
        String javacPath,
        String parameters,
        String classpath,
        String processorPath
) {
}
