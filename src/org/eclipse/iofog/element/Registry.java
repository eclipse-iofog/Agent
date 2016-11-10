package com.iotracks.iofog.element;

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
	private boolean requiersCertificate;
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

	public boolean isRequiersCertificate() {
		return requiersCertificate;
	}

	public void setRequiersCertificate(boolean requiersCertificate) {
		this.requiersCertificate = requiersCertificate;
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
		Registry other = ((Registry) o);
        return this.url.equalsIgnoreCase(other.url) && this.userEmail.equalsIgnoreCase(other.userEmail);
    }
}
