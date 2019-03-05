package org.eclipse.iofog.security_manager;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;

import java.util.List;

public class PluginRunnable implements Runnable {
    private final PluginProcessData pluginProcessData;
    private final SecurityManager securityManager;

    PluginRunnable(PluginProcessData pluginProcessData, SecurityManager securityManager) {
        this.pluginProcessData = pluginProcessData;
        this.securityManager = securityManager;
    }

    public void run() {
        securityManager.logInfo("Launched plugin '" + pluginProcessData.getJarPath() + "'");
        launchJar(pluginProcessData);

        while (true) {
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
                }
            } catch (InterruptedException e) {
                securityManager.logWarning(e.getMessage());
            } catch (Exception e) {
                securityManager.logWarning(e.getMessage());
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

        CommandShellResultSet<List<String>, List<String>> resultSet = new CommandShellResultSet<>(pluginProcessData.getResultBuffer(), null);
        CommandShellExecutor.executeDynamicCommand(
                command,
                resultSet,
                pluginProcessData.getPluginRun(),
                null
        );
    }
}
