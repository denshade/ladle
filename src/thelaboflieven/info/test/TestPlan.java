package thelaboflieven.info.test;

import java.util.List;

public record TestPlan(
        List<String> commands,
        int testClassCount,
        String javaPath,
        String classpath,
        String runner
) {
}
