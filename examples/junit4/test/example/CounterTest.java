package example;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CounterTest {
    @Test
    public void incrementsByOne() {
        assertEquals(2, new Counter().increment(1));
    }
}
