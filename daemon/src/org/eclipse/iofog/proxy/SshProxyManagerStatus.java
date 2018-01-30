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

    private String user;
    private String host;
    private int rport;
    private int lport;
    private ConnectionStatus status;
    private String errorMessage;

    public SshProxyManagerStatus() {}

    public SshProxyManagerStatus setUser(String user) {
        this.user = user;
        return this;
    }

    public SshProxyManagerStatus setHost(String host) {
        this.host = host;
        return this;
    }

    public SshProxyManagerStatus setRemotePort(int rport) {
        this.rport = rport;
        return this;
    }

    public SshProxyManagerStatus setLocalPort(int lport) {
        this.lport = lport;
        return this;
    }

    public SshProxyManagerStatus setConnectionStatus(ConnectionStatus status) {
        this.status = status;
        return this;
    }

    public SshProxyManagerStatus setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public String getJsonProxyStatus() {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                    .add("user", this.user)
                    .add("host", this.host)
                    .add("rport", this.rport)
                    .add("lport", this.lport)
                    .add("status", this.status.toString())
                    .add("errorMessage", this.errorMessage);
        return objectBuilder.build().toString();
    }
}
