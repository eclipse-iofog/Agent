package org.eclipse.iofog.proxy;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.ByteArrayInputStream;

import static org.eclipse.iofog.proxy.ConnectionStatus.*;

public class SshProxyManager {
    private String MODULE_NAME = "SSH Proxy Manager";
    private static final String LOCAL_HOST = "localhost";
    private static SshProxyManager instance;
    private static final int TIMEOUT = 60000;
    private JSch jschSSHChannel;
    private String user;
    private String password;
    private String host;
    private String rsaKey;
    private int rport;
    private int lport = 22;
    private Session session;
    private StringBuilder errorMessage = new StringBuilder();
    private boolean isConfigUpdated = false;

    private SshProxyManager() {
        jschSSHChannel = new JSch();
    }

    public static SshProxyManager getInstance() {
        if (instance == null) {
            synchronized (SshProxyManager.class) {
                if (instance == null)
                    instance = new SshProxyManager();
            }
        }
        return instance;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getRport() {
        return rport;
    }

    public int getLport() {
        return lport;
    }

    public String getRsaKey() {
        return rsaKey;
    }

    public boolean isConfigUpdated() {
        return isConfigUpdated;
    }

    public void setUser(String user) {
        this.user = user;
        this.isConfigUpdated = true;
    }

    public void setPassword(String password) {
        this.password = password;
        this.isConfigUpdated = true;
    }

    public void setHost(String host) {
        this.host = host;
        this.isConfigUpdated = true;
    }

    public void setRport(int rport) {
        this.rport = rport;
        this.isConfigUpdated = true;
    }

    public void setLport(int lport) {
        this.lport = lport;
        this.isConfigUpdated = true;
    }

    public void setRsaKey(String rsaKey) {
        this.rsaKey = rsaKey;
        this.isConfigUpdated = true;
    }

    private void setKnownHost() {
        try {
            jschSSHChannel.setKnownHosts(new ByteArrayInputStream(this.rsaKey.getBytes()));
        } catch (JSchException jschX) {
            String errMsg = String.format("There was an issue with server RSA key setup:%n %s", jschX.getMessage());
            errorMessage.append(errMsg).append(System.getProperty("line.separator"));
            LoggingService.logWarning(MODULE_NAME, errMsg);
        }
    }

    public Runnable connect() {
        return () -> {
            try {
                session = jschSSHChannel.getSession(user, host, lport);
                session.setPassword(password);

                session.connect(TIMEOUT);
                session.setPortForwardingR(host, rport, LOCAL_HOST, lport);

                setSshProxyManagerStatus(OPEN);
            } catch (JSchException jschX) {
                String errMsg = String.format("Unable to connect to the server:%n %s", jschX.getMessage());
                errorMessage.append(errMsg).append(System.getProperty("line.separator"));
                LoggingService.logWarning(MODULE_NAME, errMsg);
                setSshProxyManagerStatus(FAILED);
            }
        };
    }

    private void setSshProxyManagerStatus(ConnectionStatus status) {
        StatusReporter.setSshProxyManagerStatus()
                .setUser(user)
                .setHost(host)
                .setRemotePort(rport)
                .setLocalPort(lport)
                .setConnectionStatus(status)
                .setErrorMessage(errorMessage.toString());
    }

    private void resetErrorMessages() {
        this.errorMessage.setLength(0);
    }

    public void close() {
        if (session != null) {
            session.disconnect();
        }
        setSshProxyManagerStatus(CLOSED);
        resetErrorMessages();
    }

    public void open() {
        resetErrorMessages();
        setKnownHost();
        new Thread(connect(), "SshProxyManager : OpenSshChannel").start();
        LoggingService.logInfo(MODULE_NAME, "opened ssh channel");
        this.isConfigUpdated = false;
    }
}
