package org.eclipse.iofog.security_manager;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;

import java.lang.reflect.Field;
import java.util.List;

public class PluginRunnable implements Runnable {
    private final PluginProcessData pluginProcessData;
    private final SecurityManager securityManager;
    private Process pluginProcess;

    PluginRunnable(PluginProcessData pluginProcessData, SecurityManager securityManager) {
        this.pluginProcessData = pluginProcessData;
        this.securityManager = securityManager;
    }

    public void run() {
        securityManager.logInfo("Launched plugin '" + pluginProcessData.getJarPath() + "'");
        launchJar(pluginProcessData);

        while (!Thread.interrupted()) {
            try {
                Thread.sleep(5000);
                List<String> results = pluginProcessData.getResultBuffer();
                if (results != null && results.size() > 0) {
                    String latestStatus = results.get(results.size() - 1).replace("\n", "");
                    if (latestStatus.contains("Quarantine caused by")) {
                        securityManager.logWarning(latestStatus);
                        securityManager.logInfo("Plugin " + pluginProcessData.getPluginName() + " status: " + PluginStatus.QUARANTINE);
                        String infoMessage;
                        if (results.size() > 1) {
                            infoMessage = results.get(results.size() - 2).replace("\n", "");
                        } else {
                            infoMessage = latestStatus;
                        }

                        securityManager.handleQuarantine(infoMessage);

                        break;
                    }

                    PluginStatus pluginStatus = PluginStatus.parse(latestStatus);
                    if (pluginStatus == PluginStatus.QUARANTINE) {
                        securityManager.handleQuarantine("");
                    }

                    securityManager.logInfo("Plugin " + pluginProcessData.getPluginName() + " status: " + pluginStatus);

                    pluginProcessData.getResultBuffer().clear();
                    pluginProcessData.getErrorBuffer().clear();
                }
            } catch (InterruptedException e) {
                destroyPluginProcess();
            }
        }
    }

    private void launchJar(PluginProcessData pluginProcessData) {
        String command;
        if (SystemUtils.IS_OS_WINDOWS) {
            command = "\"java -Xmx512m -jar '" + pluginProcessData.getJarPath() + "' \"";
        } else {
            command = "java -Xmx512m -jar '" + pluginProcessData.getJarPath() + "' ";
        }

        CommandShellResultSet<List<String>, List<String>> resultSet = new CommandShellResultSet<>(pluginProcessData.getResultBuffer(), pluginProcessData.getErrorBuffer());
        pluginProcess = CommandShellExecutor.executeDynamicCommand(
            command,
            resultSet,
            pluginProcessData.getPluginRun(),
            null
        );
    }

    private void destroyPluginProcess() {
        securityManager.logInfo(String.format("stopping plugin %s...", pluginProcessData.getPluginName()));
        int parentPid = getPid(pluginProcess);
        List<String> childPids = getChildPids(parentPid);
        if (pluginProcess != null) {
            pluginProcess.destroy();
            if (parentPid != -1 && childPids != null) {
                killHaltChildProcesses(childPids);
            }
            securityManager.logInfo((String.format("plugin %s has been stopped", pluginProcessData.getPluginName())));
        }
    }

    /**
     * get the PID on unix/linux systems
     * @param process process
     * @return pid
     */
    private int getPid(Process process) {
        int pid = -1;
        if(process.getClass().getName().equals("java.lang.UNIXProcess")) {
            try {
                Field f = process.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getInt(process);
                System.out.println(pid);
            } catch (Exception e) {
                securityManager.logWarning("unable to get pid of the process");
            }
        }
        return pid;
    }

    private List<String> getChildPids(int parentPid) {
        return CommandShellExecutor.executeCommand(String.format("pgrep -P %d", parentPid)).getValue();
    }

    private void killHaltChildProcesses(List<String> childPids) {
        childPids.forEach(value -> CommandShellExecutor.executeCommand(String.format("kill -9 %s", value)));
    }
}
