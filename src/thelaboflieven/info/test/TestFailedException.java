package thelaboflieven.info.test;

public class TestFailedException extends RuntimeException {
    private final int exitCode;

    public TestFailedException(int exitCode) {
        super("Tests failed with exit code " + exitCode);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
