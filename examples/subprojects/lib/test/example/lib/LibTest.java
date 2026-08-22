package example.lib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LibTest {
    @Test
    void greets() {
        assertEquals("hello from lib", Lib.greet());
    }
}
