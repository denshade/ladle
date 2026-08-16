package thelaboflieven.info.download;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DependenciesTest {
    @Test
    void localPathsFromJarNameKey() {
        var section = Map.of(
                "lib-1.0.jar",
                "https://repo1.maven.org/maven2/some/lib/1.0/lib-1.0.jar"
        );

        assertEquals(
                java.util.List.of("dependencies/lib-1.0.jar"),
                Dependencies.localPathsFromSection(section)
        );
    }

    @Test
    void localPathsFromPackageNameKey() {
        var section = Map.of(
                "net.bytebuddy",
                "https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.0/byte-buddy-1.0.jar"
        );

        assertEquals(
                java.util.List.of("dependencies/byte-buddy-1.0.jar"),
                Dependencies.localPathsFromSection(section)
        );
    }

    @Test
    void implementationPathsFromIni() {
        var iniData = Map.of(
                Dependencies.IMPLEMENTATION,
                Map.of(
                        "a.jar", "https://example.com/a.jar",
                        "org.example.api", "https://example.com/b.jar"
                )
        );

        assertEquals(
                Set.of("dependencies/a.jar", "dependencies/b.jar"),
                Set.copyOf(Dependencies.implementationPaths(iniData))
        );
    }

    @Test
    void compileOnlyPathsFromIni() {
        var iniData = Map.of(
                Dependencies.COMPILE_ONLY,
                Map.of(
                        "org.jspecify", "https://example.com/jspecify-1.0.jar",
                        "hamcrest.jar", "https://example.com/hamcrest.jar"
                )
        );

        assertEquals(
                Set.of("dependencies/jspecify-1.0.jar", "dependencies/hamcrest.jar"),
                Set.copyOf(Dependencies.compileOnlyPaths(iniData))
        );
    }

    @Test
    void annotationProcessorPathsFromIniSkipProcessorClassKey() {
        var iniData = Map.of(
                Dependencies.ANNOTATION_PROCESSOR,
                Map.of(
                        "auto-service", "https://example.com/auto-service-1.1.1.jar",
                        "processor", "com.google.auto.service.processor.AutoServiceProcessor"
                )
        );

        assertEquals(
                java.util.List.of("dependencies/auto-service-1.1.1.jar"),
                Dependencies.annotationProcessorPaths(iniData)
        );
        assertEquals(
                "com.google.auto.service.processor.AutoServiceProcessor",
                Dependencies.annotationProcessorClasses(iniData)
        );
    }

    @Test
    void artifactsIncludeAnnotationProcessorJarsButNotProcessorClassKey() {
        var iniData = Map.of(
                Dependencies.ANNOTATION_PROCESSOR,
                Map.of(
                        "auto-service", "https://example.com/auto-service-1.1.1.jar",
                        "processor", "com.google.auto.service.processor.AutoServiceProcessor"
                )
        );

        var artifacts = Dependencies.artifacts(iniData);

        assertEquals(1, artifacts.size());
        assertEquals("https://example.com/auto-service-1.1.1.jar", artifacts.get(0).url());
        assertEquals("auto-service-1.1.1.jar", artifacts.get(0).fileName());
    }

    @Test
    void fileNameFromUrlStripsQueryString() {
        assertEquals(
                "jdk.zip",
                Dependencies.fileNameFromUrl("https://example.com/jdk.zip?token=abc"));
    }
}
