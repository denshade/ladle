package thelaboflieven.info.download;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImplementationDependenciesTest {
    @Test
    void localPathsFromJarNameKey() {
        var dependencies = Map.of(
                "lib-1.0.jar",
                "https://repo1.maven.org/maven2/some/lib/1.0/lib-1.0.jar"
        );

        assertEquals(
                java.util.List.of("dependencies/lib-1.0.jar"),
                ImplementationDependencies.localPathsFromSection(dependencies)
        );
    }

    @Test
    void localPathsFromPackageNameKey() {
        var dependencies = Map.of(
                "net.bytebuddy",
                "https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.0/byte-buddy-1.0.jar"
        );

        assertEquals(
                java.util.List.of("dependencies/byte-buddy-1.0.jar"),
                ImplementationDependencies.localPathsFromSection(dependencies)
        );
    }

    @Test
    void localPathsFromIni() {
        var iniData = Map.of(
                "dependencies",
                Map.of(
                        "a.jar", "https://example.com/a.jar",
                        "org.example.api", "https://example.com/b.jar"
                )
        );

        assertEquals(
                Set.of("dependencies/a.jar", "dependencies/b.jar"),
                Set.copyOf(ImplementationDependencies.localPaths(iniData))
        );
    }
}
