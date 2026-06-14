package thelaboflieven.info.inifile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IniEnvironmentTest {
    @Test
    void expandsBracedEnvironmentVariable() {
        var original = System.getenv("PATH");
        if (original == null) {
            return;
        }
        assertEquals(original, IniEnvironment.expand("${PATH}"));
    }

    @Test
    void expandsSimpleEnvironmentVariable() {
        var original = System.getenv("PATH");
        if (original == null) {
            return;
        }
        assertEquals(original, IniEnvironment.expand("$PATH"));
    }

    @Test
    void detectsEnvironmentReferences() {
        assertTrue(IniEnvironment.referencesEnvironment("$JAVA_HOME"));
        assertTrue(IniEnvironment.referencesEnvironment("${JAVA_HOME}/bin"));
        assertFalse(IniEnvironment.referencesEnvironment(".jdk"));
    }

    @Test
    void failsWhenEnvironmentVariableMissing() {
        assertThrows(IllegalStateException.class, () -> IniEnvironment.expand("$LADLE_TEST_MISSING_VAR"));
    }
}
