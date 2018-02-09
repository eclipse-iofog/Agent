package org.eclipse.iofog.proxy;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.ByteArrayInputStream;

import static org.eclipse.iofog.proxy.ConnectionStatus.*;

/**
 * SSH Proxy Manager Module
 *
 * @author epankov
 */
public class SshProxyManager {
    private String MODULE_NAME = "SSH Proxy Manager";
    private static final String LOCAL_HOST = "localhost";
    private static SshProxyManager instance;
    private static final int TIMEOUT = 60000;
    private JSch jschSSHChannel;
    private String username;
    private String password;
    private String host;
    private String rsaKey;
    private int rport;
    private int lport = 22;
    private boolean closeFlag;
    private Session session;
    private StringBuilder errorMessage = new StringBuilder();

    public SshProxyManager() {
        jschSSHChannel = new JSch();
    }

    public void setProxyInfo(String username, String password, String host, int rport, int lport, String rsaKey, boolean closeFlag) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.rport = rport;
        this.lport = lport;
        this.rsaKey = rsaKey;
        this.closeFlag = closeFlag;
    }

    /**
     * opens reverse proxy on specified host
     *
     * @return Runnable to be executed on separate thread
     */
    public Runnable connect() {
        return () -> {
            try {
                session = jschSSHChannel.getSession(username, host, lport);
                session.setPassword(password);

                session.connect(TIMEOUT);
                session.setPortForwardingR(host, rport, LOCAL_HOST, lport);

                setSshProxyManagerStatus(OPEN);
            } catch (JSchException jschX) {
                String errMsg = String.format("Unable to connect to the server:%n %s", jschX.getMessage());
                updateProxyManagerStatus(errMsg, FAILED);
                session.disconnect();
            }
        };
    }

    /**
     * starts or stops ssh tunnel according to current config
     */
    public void update() {
        if (isConnected() && closeFlag) {
            close();
        } else if (!isConnected() && !closeFlag){
            open();
        } else if (isConnected() && !closeFlag) {
            handleUnexpectedTunnelState("The tunnel is already opened. Please close it first.", OPEN);
        } else {
            handleUnexpectedTunnelState("The tunnel is already closed", CLOSED);
        }
    }


    /**
     * handles unexpected situations like tunnel is already opened or closed
     * @param errMsg error message
     * @param status connection status
     */
    private void handleUnexpectedTunnelState(String errMsg, ConnectionStatus status) {
        resetErrorMessages();
        updateProxyManagerStatus(errMsg, status);
    }

    /**
     * opens ssh tunnel
     */
    private void open() {
        resetErrorMessages();
        setKnownHost();
        new Thread(connect(), "SshProxyManager : OpenSshChannel").start();
        LoggingService.logInfo(MODULE_NAME, "opened ssh tunnel");
    }

    /**
     * closes ssh tunnel
     */
    private void close() {
        resetErrorMessages();
        session.disconnect();
        setSshProxyManagerStatus(CLOSED);
        LoggingService.logInfo(MODULE_NAME, "closed ssh tunnel");
    }

    /**
     * adds server rsa key to known hosts
     */
    private void setKnownHost() {
        try {
            jschSSHChannel.setKnownHosts(new ByteArrayInputStream(this.rsaKey.getBytes()));
        } catch (JSchException jschX) {
            String errMsg = String.format("There was an issue with server RSA key setup:%n %s", jschX.getMessage());
            errorMessage.append(errMsg).append(System.getProperty("line.separator"));
            LoggingService.logWarning(MODULE_NAME, errMsg);
        }
    }

    /**
     * sets current ssh proxy manager status
     *
     * @param status Connection status of the tunnel
     */
    private void setSshProxyManagerStatus(ConnectionStatus status) {
        StatusReporter.setSshProxyManagerStatus()
                .setUsername(username)
                .setHost(host)
                .setRemotePort(rport)
                .setLocalPort(lport)
                .setConnectionStatus(status)
                .setErrorMessage(errorMessage.toString());
    }

    /**
     * resets all the errors
     */
    private void resetErrorMessages() {
        this.errorMessage.setLength(0);
    }

    /**
     * checks if tunnel is open
     *
     * @return boolean
     */
    private boolean isConnected() {
        return this.session != null && session.isConnected();
    }

    /**
     * updates ssh proxy manager status
     *
     * @param errMsg error message
     * @param status connection status
     */
    private void updateProxyManagerStatus(String errMsg, ConnectionStatus status) {
        errorMessage.append(errorMessage).append(System.getProperty("line.separator"));
        LoggingService.logWarning(MODULE_NAME, errMsg);
        setSshProxyManagerStatus(status);
    }
}
