package thelaboflieven.info;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandLine {
    private static final int ARGFILE_THRESHOLD_CHARS = 7000;

    private CommandLine() {
    }

    public static List<String> splitParameters(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return List.of();
        }
        var tokens = new ArrayList<String>();
        var current = new StringBuilder();
        var inQuotes = false;
        for (int i = 0; i < parameters.length(); i++) {
            var ch = parameters.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (!inQuotes && Character.isWhitespace(ch)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    public static String format(List<String> argv) {
        return argv.stream().map(CommandLine::quote).collect(Collectors.joining(" "));
    }

    public static List<String> javacCommand(
            String executable,
            List<String> arguments,
            File projectDir,
            String argfileRelativePath
    ) throws IOException {
        if (!needsArgfile(executable, arguments)) {
            var command = new ArrayList<String>();
            command.add(executable);
            command.addAll(arguments);
            return command;
        }

        var argfile = new File(projectDir, argfileRelativePath);
        writeArgfile(argfile.toPath(), arguments);
        var argfileArgument = "@" + ProjectPaths.relativeTo(projectDir, argfile);
        return List.of(executable, argfileArgument);
    }

    public static void writeArgfile(Path argfile, List<String> arguments) throws IOException {
        var parent = argfile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        var content = new StringBuilder();
        for (var argument : arguments) {
            content.append(argument).append('\n');
        }
        Files.writeString(argfile, content.toString());
    }

    public static boolean needsArgfile(String executable, List<String> arguments) {
        var length = executable.length();
        for (var argument : arguments) {
            length += 1 + argument.length();
        }
        return length > ARGFILE_THRESHOLD_CHARS;
    }

    private static String quote(String argument) {
        if (argument.contains(" ") || argument.contains("\"")) {
            return "\"" + argument.replace("\"", "\\\"") + "\"";
        }
        return argument;
    }
}
