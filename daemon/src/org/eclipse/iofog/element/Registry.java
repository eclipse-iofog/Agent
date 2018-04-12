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
	private final String url;
	private final boolean secure;
	private final String certificate;
	private final boolean requiresCertificate;
	private final String userName;
	private final String password;
	private final String userEmail;

	private Registry(final String url, final boolean secure, final String certificate, final boolean requiresCertificate,
	                 final String userName, final String password, final String userEmail) {
		this.url = url;
		this.secure = secure;
		this.certificate = certificate;
		this.requiresCertificate = requiresCertificate;
		this.userName = userName;
		this.password = password;
		this.userEmail = userEmail;
	}

	public String getUrl() {
		return url;
	}

	public boolean isSecure() {
		return secure;
	}

	public String getCertificate() {
		return certificate;
	}

	public boolean isRequiresCertificate() {
		return requiresCertificate;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getUserEmail() {
		return userEmail;
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

	public static class RegistryBuilder {
		private String url;
		private boolean secure;
		private String certificate;
		private boolean requiresCertificate;
		private String userName;
		private String password;
		private String userEmail;

		public RegistryBuilder setUrl(String url) {
			this.url = url;
			return this;
		}

		public RegistryBuilder setSecure(boolean secure) {
			this.secure = secure;
			return this;
		}

		public RegistryBuilder setCertificate(String certificate) {
			this.certificate = certificate;
			return this;
		}

		public RegistryBuilder setRequiresCertificate(boolean requiresCertificate) {
			this.requiresCertificate = requiresCertificate;
			return this;
		}

		public RegistryBuilder setUserName(String userName) {
			this.userName = userName;
			return this;
		}

		public RegistryBuilder setPassword(String password) {
			this.password = password;
			return this;
		}

		public RegistryBuilder setUserEmail(String userEmail) {
			this.userEmail = userEmail;
			return this;
		}

		public Registry build() {
			return new Registry(url, secure, certificate, requiresCertificate, userName, password, userEmail);
		}
	}
}
