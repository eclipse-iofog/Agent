/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.microservice;

import java.util.Objects;

/**
 * represents registries
 * 
 * @author saeid
 *
 */
public class Registry {
	private final int id;
	private final String url;
	private final boolean isPublic;
	private final String userName;
	private final String password;
	private final String userEmail;

	private Registry(final int id, final String url, final boolean isPublic, final String userName, final String password, final String userEmail) {
		this.id = id;
		this.url = url;
		this.isPublic = isPublic;
		this.userName = userName;
		this.password = password;
		this.userEmail = userEmail;
	}

	public int getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public boolean getIsPublic() {
		return isPublic;
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

		if (isPublic != registry.isPublic) return false;
		return id == registry.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(url, isPublic);
	}

	public static class RegistryBuilder {
		private int id;
		private String url;
		private boolean isPublic;
		private String userName;
		private String password;
		private String userEmail;

		public RegistryBuilder setId(int id) {
			this.id = id;
			return this;
		}

		public RegistryBuilder setUrl(String url) {
			this.url = url;
			return this;
		}

		public RegistryBuilder setIsPublic(boolean isPublic) {
			this.isPublic = isPublic;
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
			return new Registry(id, url, isPublic, userName, password, userEmail);
		}
	}
}
