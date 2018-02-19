package org.eclipse.iofog.proxy;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

/**
 * SSH Proxy Manager Status
 *
 * @author epankov
 *
 */
public class SshProxyManagerStatus {

    private String username = "";
    private String host = "";
    private int rport = 0;
    private int lport = 0;
    private SshConnectionStatus status = SshConnectionStatus.CLOSED;
    private String errorMessage = "";

    public SshProxyManagerStatus() {}

    public synchronized SshProxyManagerStatus setUsername(String username) {
        this.username = username;
        return this;
    }

    public synchronized SshProxyManagerStatus setHost(String host) {
        this.host = host;
        return this;
    }

    public synchronized SshProxyManagerStatus setRemotePort(int rport) {
        this.rport = rport;
        return this;
    }

    public synchronized SshProxyManagerStatus setLocalPort(int lport) {
        this.lport = lport;
        return this;
    }

    public synchronized SshProxyManagerStatus setConnectionStatus(SshConnectionStatus status) {
        this.status = status;
        return this;
    }

    public synchronized SshProxyManagerStatus setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public synchronized String getJsonProxyStatus() {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                    .add("username", this.username)
                    .add("host", this.host)
                    .add("rport", this.rport)
                    .add("lport", this.lport)
                    .add("status", this.status.toString())
                    .add("errormessage", this.errorMessage);
        return objectBuilder.build().toString();
    }
}
