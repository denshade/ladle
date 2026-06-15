package thelaboflieven.info;

import java.io.*;
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
        var process = new ProcessBuilder(command).directory(currentWorkingDir).start();
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
        errorGobbler.start();
        outputGobbler.start();
        int exitCode = process.waitFor();
        errorGobbler.join();
        outputGobbler.join();
        return exitCode;
    }

    class StreamGobbler extends Thread {
        InputStream is;

        StreamGobbler(InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
