package thelaboflieven.info;

public class CommandFailedException extends RuntimeException {
    private final int exitCode;

    public CommandFailedException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public static CommandFailedException build(int exitCode) {
        return new CommandFailedException("Build failed with exit code " + exitCode, exitCode);
    }

    public static CommandFailedException test(int exitCode) {
        return new CommandFailedException("Tests failed with exit code " + exitCode, exitCode);
    }

    public int exitCode() {
        return exitCode;
    }
}
