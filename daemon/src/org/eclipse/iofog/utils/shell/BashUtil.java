package org.eclipse.iofog.utils.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BashUtil {
    private BashUtil() {
    }

    public static String ERROR_WHILE_BASH_COMMAND_EXECUTION = "error while script execution";

    public static String executeShellScript(String command) throws InterruptedException, IOException {
        return executeShellScript(splitCommandToParts(command));
    }

    public static String executeShellScript(String[] commandsParts) throws InterruptedException, IOException {
        String[] fullCommand = prepareScriptToExec(commandsParts);
        return execute(fullCommand);
    }

    public static String executeShellCommand(String command) throws InterruptedException, IOException {
        String[] fullCommand = prepareCommandToExec(command);
        return execute(fullCommand);
    }

    private static String execute(String[] fullCommand) throws InterruptedException, IOException {

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        Process p = pb.start();
        return getBashResponse(p);
    }

    private static String getBashResponse(Process p) throws InterruptedException, IOException {
        StringBuffer res = new StringBuffer();
        p.waitFor();
        if (p.exitValue() != 0) {
            return ERROR_WHILE_BASH_COMMAND_EXECUTION;
        }

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String t;
        while ((t = stdInput.readLine()) != null) {
            res.append(t);
        }
        return res.toString();
    }

    private static String[] splitCommandToParts(String command) {
        String[] commands = command.split(" ");
        commands = Arrays.stream(commands)
                .map(String::trim)
                .toArray(String[]::new);
        return commands;
    }

    private static String[] prepareCommandToExec(String command) {
        String[] res = {"/bin/bash", "-c", command};
        return res;
    }

    private static String[] prepareScriptToExec(String command) {
        String[] commandParts = splitCommandToParts(command);
        return prepareScriptToExec(commandParts);
    }

    private static String[] prepareScriptToExec(String[] commandParts) {
        List<String> res = new ArrayList<>(Arrays.asList(commandParts));
        res.add(0, "/bin/bash");
        return res.toArray(new String[res.size()]);
    }
}
