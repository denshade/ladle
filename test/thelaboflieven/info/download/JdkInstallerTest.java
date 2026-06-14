package thelaboflieven.info.download;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JdkInstallerTest {
    @Test
    void isConfiguredWhenPathPresent() {
        var iniData = Map.of("javac", Map.of("path", ".jdk"));
        assertTrue(JdkInstaller.isConfigured(iniData));
    }

    @Test
    void isConfiguredWhenDownloadUrlPresent() {
        var iniData = Map.of("javac", Map.of("download.linux", "https://example.com/jdk.tar.gz"));
        assertTrue(JdkInstaller.isConfigured(iniData));
    }

    @Test
    void isNotConfiguredWithoutJavacSection() {
        assertFalse(JdkInstaller.isConfigured(Map.of()));
    }
}
