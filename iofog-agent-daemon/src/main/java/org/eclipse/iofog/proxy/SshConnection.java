/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2022 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog.proxy;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.ByteArrayInputStream;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SshConnection {

	private static final String LOCAL_HOST = "localhost";
	private static final int TIMEOUT = 60000;
	private final JSch jschSSHChannel;
	private String username;
	private String password;
	private String host;
	private String rsaKey;
	private int remotePort;
	private int localPort = 22;
	private boolean closeFlag;
	private Session session;

	public SshConnection() {
		this.jschSSHChannel = new JSch();
	}

	/**
	 * opens reverse proxy on specified host
	 */
	public synchronized Supplier<Void> openSshTunnel() {
		return () -> {
			try {
				session = jschSSHChannel.getSession(username, host, localPort);
				session.setPassword(password);
				session.connect(TIMEOUT);
				session.setPortForwardingR(host, remotePort, LOCAL_HOST, localPort);
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
	public synchronized void setProxyInfo(String username, String password, String host, int rport, int lport, String rsaKey, boolean closeFlag) {
		this.username = username;
		this.password = password;
		this.host = host;
		this.remotePort = rport;
		this.localPort = lport;
		this.rsaKey = rsaKey;
		this.closeFlag = closeFlag;
	}

	/**
	 * adds server rsa key to known hosts
	 */
	public void setKnownHost() throws JSchException {
		jschSSHChannel.setKnownHosts(new ByteArrayInputStream(this.rsaKey.getBytes(UTF_8)));
	}

	/**
	 * checks if tunnel is open
	 *
	 * @return boolean
	 */
	public synchronized boolean isConnected() {
		return this.session != null && session.isConnected();
	}

	public synchronized boolean isCloseFlag() {
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

	public int getRemotePort() {
		return remotePort;
	}

	public int getLocalPort() {
		return localPort;
	}
}
