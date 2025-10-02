package thelaboflieven.info;

import java.io.*;
import java.util.List;

public class CommandsRunner {
    private final File currentWorkingDir;

    public CommandsRunner(File currentWorkingDir){
        this.currentWorkingDir = currentWorkingDir;
    }

    void run(List<String> commands) throws IOException, InterruptedException {
        for (String command : commands) {
            var process = new ProcessBuilder().command(command.split(" ")).directory(currentWorkingDir).start();
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
            errorGobbler.start();
            outputGobbler.start();
            process.waitFor();
        }
    }

    class StreamGobbler extends Thread {
        InputStream is;

        // reads everything from is until empty.
        StreamGobbler(InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line=null;
                while ( (line = br.readLine()) != null)
                    System.out.println(line);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
