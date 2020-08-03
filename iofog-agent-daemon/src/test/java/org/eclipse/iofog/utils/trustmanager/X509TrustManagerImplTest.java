/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2020 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.utils.trustmanager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({X509TrustManagerImpl.class, Certificate.class})
@PowerMockIgnore({"java.net.ssl", "javax.security.auth.x500.X500Principal"})
public class X509TrustManagerImplTest {
    private Certificate certificate;
    private X509Certificate[] certificates;
    private X509Certificate x509Certificate;
    private X509TrustManagerImpl x509TrustManager;

    @Before
    public void setUp() throws Exception {
        certificate = mock(Certificate.class);
        x509Certificate = mock(X509Certificate.class);
        certificates = new X509Certificate[1];
        certificates[0] = x509Certificate;
        x509TrustManager = spy(new X509TrustManagerImpl(certificate));
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test checkServerTrusted when certificate is null
     * @throws CertificateException
     */
    @Test (expected = CertificateException.class)
    public void throwsExceptionWhenCertificateIsNullInCheckServerTrusted() throws CertificateException {
        x509TrustManager.checkServerTrusted(null, null);
    }

    /**
     * Test checkServerTrusted when certificate is null
     * @throws CertificateException
     */
    @Test (expected = CertificateException.class)
    public void throwsExceptionWhenCertificateIsNotEqualInCheckServerTrusted() throws CertificateException {
        x509TrustManager.checkServerTrusted(certificates, "args");
    }

    /**
     * When certificate is equal
     */
    @Test
    public void testCheckServerTrusted() {
        try {
            certificate = x509Certificate;
            x509TrustManager = spy(new X509TrustManagerImpl(certificate));
            x509TrustManager.checkServerTrusted(certificates, "args");
        } catch (CertificateException e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getAcceptedIssuers
     */
    @Test
    public void getAcceptedIssuers() {
        assertNotNull(x509TrustManager.getAcceptedIssuers());
    }
}