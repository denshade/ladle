package example.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppTest {
    @Test
    void greetsSampleName() {
        assertEquals("Hello, ladle", App.greet(AppFixture.sampleName()));
    }
}
