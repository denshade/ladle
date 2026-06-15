package thelaboflieven.info.test;

import java.util.List;

public record TestPlan(
        List<List<String>> commands,
        int testClassCount,
        String javaPath,
        String classpath,
        String runner
) {
}
