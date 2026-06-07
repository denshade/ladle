package thelaboflieven.info.build;

public class BuildFailedException extends RuntimeException {
    private final int exitCode;

    public BuildFailedException(int exitCode) {
        super("Build failed with exit code " + exitCode);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
