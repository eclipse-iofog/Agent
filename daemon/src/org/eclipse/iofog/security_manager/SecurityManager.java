package org.eclipse.iofog.security_manager;


import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.field_agent.FieldAgent;
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
            PluginProcessData pluginProcessData = new PluginProcessData(pathToJar);
            PluginRunnable pluginRunnable = new PluginRunnable(pluginProcessData, this);
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


    public void handleQuarantine(String quarantineInfoMessage) {
        if (!StatusReporter.getSecurityStatus().getStatus().equals(SecurityStatus.Status.QUARANTINE)) {
            FieldAgent.getInstance().startQuarantine(quarantineInfoMessage);
        }
    }
}
