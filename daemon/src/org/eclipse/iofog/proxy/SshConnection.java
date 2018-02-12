package org.eclipse.iofog.proxy;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.ByteArrayInputStream;
import java.util.function.Supplier;

public class SshConnection {

	private static final String LOCAL_HOST = "localhost";
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

	public SshConnection() {
		this.jschSSHChannel = new JSch();
	}

	/**
	 * opens reverse proxy on specified host
	 */
	public Supplier<Void> openSshTunnel() {
		return () -> {
			try {
				session = jschSSHChannel.getSession(username, host, lport);
				session.setPassword(password);
				session.connect(TIMEOUT);
				session.setPortForwardingR(host, rport, LOCAL_HOST, lport);
			} catch (JSchException jschX) {
				session.disconnect();
				throw new RuntimeException(jschX.getMessage());
			}
			return null;
		};

	}

	/**
	 * sets connection info
	 * @param username username
	 * @param password user password
	 * @param host	host name to start up proxy server
	 * @param rport	remote port
	 * @param lport	local port
	 * @param rsaKey server rsa key
	 * @param closeFlag flag indicating if connection should be closed or opened
	 */
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
	 * adds server rsa key to known hosts
	 */
	public void setKnownHost() throws JSchException {
		jschSSHChannel.setKnownHosts(new ByteArrayInputStream(this.rsaKey.getBytes()));
	}

	/**
	 * checks if tunnel is open
	 *
	 * @return boolean
	 */
	public boolean isConnected() {
		return this.session != null && session.isConnected();
	}

	public boolean isCloseFlag() {
		return closeFlag;
	}

	public Session getSession() {
		return session;
	}

	public String getUsername() {
		return username;
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
}
