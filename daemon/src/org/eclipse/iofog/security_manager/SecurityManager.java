package org.eclipse.iofog.security_manager;


import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.iofog.utils.Constants.PLUGINS_PATH;


public class SecurityManager implements IOFogModule {
    private final String MODULE_NAME = "SecurityManager";

    private static SecurityManager instance = null;

    private boolean isQuarantine = false;

    public static SecurityManager getInstance() {
        if (instance == null) {
            synchronized (SecurityManager.class) {
                if (instance == null)
                    instance = new SecurityManager();
            }
        }
        return instance;
    }

    @Override
    public void start() throws Exception {
        if (!SystemUtils.IS_OS_WINDOWS) { // increase number of inotify watches
            CommandShellExecutor.executeCommand("echo 1000000 > /proc/sys/fs/inotify/max_user_watches");
        }

        List<String> jars = detectAllAvailableJars();
        launchJars(jars);
    }

    private void launchJars(List<String> pathsToJar) {
        for (String pathToJar : pathsToJar) {
            PluginProcessData pluginProcessData = new PluginProcessData(pathToJar, true);
            PluginRunnable pluginRunnable = new PluginRunnable(pluginProcessData);
            new Thread(pluginRunnable, "SecurityManager : LaunchPlugin " + pathToJar).start();
        }
    }

    private List<String> detectAllAvailableJars() {
        try (Stream<Path> paths = Files.walk(Paths.get(PLUGINS_PATH))) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    @Override
    public int getModuleIndex() {
        return Constants.SECURITY_MANAGER;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    public class PluginRunnable implements Runnable {
        private final PluginProcessData pluginProcessData;

        PluginRunnable(PluginProcessData pluginProcessData) {
            this.pluginProcessData = pluginProcessData;
        }

        public void run() {
            logInfo("Launched plugin '" + pluginProcessData.getJarPath() + "'");
            launchJar(pluginProcessData);

            while (true) {
                try {
                    Thread.sleep(5000);
                    List<String> results = pluginProcessData.getResultBuffer();
                    if (results != null && results.size() > 0) {
                        String latestStatus = results.get(results.size() - 1).replace("\n", "");
                        if (latestStatus.contains("Quarantine caused by")) {
                            logWarning(latestStatus);
                            logInfo("Plugin " + pluginProcessData.getPluginName() + " status: " + PluginStatus.QUARANTINE);
                            handleQuarantine();
                            continue;
                        }

                        PluginStatus pluginStatus = PluginStatus.parse(latestStatus);
                        if (pluginStatus == PluginStatus.QUARANTINE) {
                            handleQuarantine();
                        }

                        logInfo("Plugin " + pluginProcessData.getPluginName() + " status: " + pluginStatus);

                        pluginProcessData.getResultBuffer().clear();
                    }
                } catch (InterruptedException e) {
                    logWarning(e.getMessage());
                } catch (Exception e) {
                    logWarning(e.getMessage());
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


    private void handleQuarantine() {
        FieldAgent.getInstance().startQuarantine();
    }
}
