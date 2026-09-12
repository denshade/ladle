package thelaboflieven.info;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CommandsRunner {
    private final File currentWorkingDir;

    public CommandsRunner(File currentWorkingDir) {
        this.currentWorkingDir = currentWorkingDir;
    }

    public int run(List<List<String>> commands) throws IOException, InterruptedException {
        for (var command : commands) {
            var exitCode = runCommand(command);
            if (exitCode != 0) {
                return exitCode;
            }
        }
        return 0;
    }

    public int runCommand(List<String> command) throws IOException, InterruptedException {
        return new ProcessBuilder(command)
                .directory(currentWorkingDir)
                .inheritIO()
                .start()
                .waitFor();
    }
}
