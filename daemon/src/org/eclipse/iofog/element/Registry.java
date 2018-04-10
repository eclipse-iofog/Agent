/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.element;

import java.util.Objects;

/**
 * represents registries
 * 
 * @author saeid
 *
 */
public class Registry {
	private String url;
	private boolean secure;
	private String certificate;
	private boolean requiresCertificate;
	private String userName;
	private String password;
	private String userEmail;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}

	public boolean isRequiresCertificate() {
		return requiresCertificate;
	}

	public void setRequiresCertificate(boolean requiresCertificate) {
		this.requiresCertificate = requiresCertificate;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Registry registry = (Registry) o;
		return Objects.equals(url, registry.url) &&
				Objects.equals(userEmail, registry.userEmail);
	}

	@Override
	public int hashCode() {
		return Objects.hash(url, userEmail);
	}
}
