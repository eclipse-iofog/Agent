package org.eclipse.iofog.proxy;

import com.jcraft.jsch.JSchException;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.functional.Unit;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.JsonObject;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.eclipse.iofog.proxy.SshConnectionStatus.*;
import static org.eclipse.iofog.utils.functional.Unit.UNIT;

/**
 * SSH Proxy Manager Module
 *
 * @author epankov
 */
public class SshProxyManager {
    private static final String MODULE_NAME = "SSH Proxy Manager";
    private static final int DEFAULT_LOCAL_PORT = 22;
    private static final int DEFAULT_REMOTE_PORT = 9999;
    private final SshConnection connection;

    public SshProxyManager(SshConnection connection) {
        this.connection = connection;
    }

    /**
     * starts or stops ssh tunnel according to current config
     * @param configs json object with proxy configs
     * @return completable future of type void
     */
    public CompletableFuture<Unit> update(JsonObject configs) {
        CompletableFuture<Unit> completableFuture = CompletableFuture.completedFuture(UNIT);
        setSshConnection(configs);
        if (connection.isConnected() && connection.isCloseFlag()) {
            close();
        } else if (!connection.isConnected() && !connection.isCloseFlag()){
            completableFuture = open();
        } else if (connection.isConnected() && !connection.isCloseFlag()) {
            handleUnexpectedTunnelState("The tunnel is already opened. Please close it first.", OPEN);
        } else {
            handleUnexpectedTunnelState("The tunnel is already closed", CLOSED);
        }
        return completableFuture;
    }


    /**
     * handles unexpected situations like tunnel is already opened or closed
     * @param errMsg error message
     * @param status connection status
     */
    private void handleUnexpectedTunnelState(String errMsg, SshConnectionStatus status) {
        LoggingService.logWarning(MODULE_NAME, errMsg);
        setSshProxyManagerStatus(status, errMsg);
    }

    /**
     * opens ssh tunnel
     * @return completable future of type void
     */
    private CompletableFuture<Unit> open() {
        setKnownHost();
        return openSshTunnel();
    }

    /**
     * adds server rsa key to known hosts
     */
    private void setKnownHost() {
        try {
            connection.setKnownHost();
        } catch (JSchException jschX) {
            String errMsg = String.format("There was an issue with server RSA key setup:%n %s", jschX.getMessage());
            LoggingService.logWarning(MODULE_NAME, errMsg);
        }
    }

    /**
     * opens ssh tunnel asynchronously
     * @return completable future of type void
     */
    private CompletableFuture<Unit> openSshTunnel() {
        return CompletableFuture.supplyAsync(connection.openSshTunnel())
                .handle((val, ex) -> {
                    if (ex != null) {
                        onError(ex);
                    } else {
                        onSuccess();
                    }
                    return null;
                });
    }

    private void onSuccess() {
        setSshProxyManagerStatus(OPEN, EMPTY);
        LoggingService.logInfo(MODULE_NAME, "opened ssh tunnel");
    }

    private void onError(Throwable ex) {
        String errMsg = String.format("Unable to connect to the server:%n %s", ex.getMessage());
        LoggingService.logWarning(MODULE_NAME, errMsg);
        setSshProxyManagerStatus(FAILED, errMsg);
    }

    /**
     * closes ssh tunnel
     */
    private void close() {
        connection.getSession().disconnect();
        setSshProxyManagerStatus(CLOSED, EMPTY);
        LoggingService.logInfo(MODULE_NAME, "closed ssh tunnel");
    }

    /**
     * sets current ssh proxy manager status
     *
     * @param status SshConnection status of the tunnel
     */
    private void setSshProxyManagerStatus(SshConnectionStatus status, String errMsg) {
        StatusReporter.setSshProxyManagerStatus()
                .setProxyConfig(connection.getUsername(), connection.getHost(), connection.getRemotePort(), connection.getLocalPort())
                .setConnectionStatus(status)
                .setErrorMessage(errMsg);
    }

    /**
     * sets proxy connection info
     * @param configs JsonObject configs for ssh proxy
     */
    private void setSshConnection(JsonObject configs) {
        String username = configs.getString("username");
        String password = configs.getString("password");
        String host = configs.getString("host");
        String rsaKey = configs.getString("rsakey");
        int rport = configs.getInt("rport", DEFAULT_REMOTE_PORT);
        int lport = configs.getInt("lport", DEFAULT_LOCAL_PORT);
        boolean closeFlag = (configs.getBoolean("close"));
        connection.setProxyInfo(username, password, host, rport, lport, rsaKey, closeFlag);
    }
}
