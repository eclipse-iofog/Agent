package org.eclipse.iofog.network;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.utils.functional.Pair;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class IOFogNetworkInterfaceManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> futureTask;

    private static final String MODULE_NAME = "IOFogNetworkInterfaceManager";
    private static IOFogNetworkInterfaceManager instance;
    private String currentIpAddress;
    private Pair<NetworkInterface, InetAddress> networkInterface;
    private String hostName;
    private long pid;

    public String getCurrentIpAddress() {
        return currentIpAddress;
    }

    public void setCurrentIpAddress(String currentIpAddress) {
        this.currentIpAddress = currentIpAddress;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public Pair<NetworkInterface, InetAddress> getNetworkInterface() {
        return networkInterface;
    }

    public void setNetworkInterface(Pair<NetworkInterface, InetAddress> networkInterface) {
        this.networkInterface = networkInterface;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getPid() {
        return pid;
    }

    private InetAddress inetAddress;
    private IOFogNetworkInterfaceManager(){}
    public static IOFogNetworkInterfaceManager getInstance() {
        if(instance == null){
            synchronized (IOFogNetworkInterfaceManager.class){
                if(instance == null) {
                    instance = new IOFogNetworkInterfaceManager();
                }
            }
        }
        return instance;
    }

    public void updateIOFogNetworkInterface() throws SocketException, MalformedURLException {
        LoggingService.logDebug(MODULE_NAME, "Updating IoFog NetworkInterface");
        try {
            setCurrentIpAddress(IOFogNetworkInterface.getCurrentIpAddress());
            setNetworkInterface(IOFogNetworkInterface.getNetworkInterface());
            setInetAddress(IOFogNetworkInterface.getInetAddress());
            setHostName(IOFogNetworkInterface.getHostName());
            setPid(getFogPid());
        } catch (SocketException | MalformedURLException exp) {
            LoggingService.logError(MODULE_NAME, "Unable to set IP address of the machine running ioFog", new AgentSystemException(exp.getMessage(), exp));
            throw exp;
        }
    }

    private final Runnable getIoFogNetworkInterface = () -> {
        LoggingService.logDebug(MODULE_NAME, "Start getIoFogNetworkInterface");
        try {
            updateIOFogNetworkInterface();
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME,"Error in updateIOFogNetworkInterface", new AgentSystemException(e.getMessage(), e));
            if (futureTask != null)
            {
                futureTask.cancel(true);
            }
            start();
        }
        LoggingService.logDebug(MODULE_NAME, "Finished getIoFogNetworkInterface");
    };
    public void start() {
        LoggingService.logInfo(MODULE_NAME, "Start IoFog NetworkInterface");
        try {
            updateIOFogNetworkInterface();
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME,"Error in updating IOFogNetworkInterface", new AgentSystemException(e.getMessage(), e));
            start();
        }
        futureTask = scheduler.scheduleAtFixedRate(getIoFogNetworkInterface, 0, 30, TimeUnit.MINUTES);
        LoggingService.logInfo(MODULE_NAME, "Started IoFog NetworkInterface");
    }

    public long getFogPid(){
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        return Long.parseLong(processName.split("@")[0]);
    }

}
