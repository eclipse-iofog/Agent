package org.eclipse.iofog.utils.trustmanager;

import javax.net.ssl.X509TrustManager;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created by ekrylovich
 * on 2/2/18.
 */
public class X509TrustManagerImpl implements X509TrustManager {
	private final Certificate controllerCert;

	public X509TrustManagerImpl(Certificate controllerCert) {
		this.controllerCert = controllerCert;
	}

	@Override
	public void checkServerTrusted(X509Certificate[] certs, String arg1) throws CertificateException {
		boolean verified = false;
		for (X509Certificate cert : certs) {
			if (cert.equals(controllerCert)) {
				verified = true;
				break;
			}
		}
		if (!verified)
			throw new CertificateException();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] certs, String arg1) throws CertificateException {
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}
}
