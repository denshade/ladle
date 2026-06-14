package thelaboflieven.info.download;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompileOnlyDependenciesTest {
    @Test
    void localPathsFromIni() {
        var iniData = Map.of(
                "compileonlydependencies",
                Map.of(
                        "org.jspecify", "https://example.com/jspecify-1.0.jar",
                        "hamcrest.jar", "https://example.com/hamcrest.jar"
                )
        );

        assertEquals(
                java.util.Set.of("dependencies/jspecify-1.0.jar", "dependencies/hamcrest.jar"),
                java.util.Set.copyOf(CompileOnlyDependencies.localPaths(iniData))
        );
    }
}
