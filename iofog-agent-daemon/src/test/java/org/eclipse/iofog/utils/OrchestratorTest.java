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
package org.eclipse.iofog.utils;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.field_agent.enums.RequestType;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.device_info.ArchitectureType;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.utils.trustmanager.TrustManagers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.naming.AuthenticationException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import java.io.*;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateFactorySpi;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Orchestrator.class, Configuration.class, SSLConnectionSocketFactory.class, LoggingService.class,
        HttpClients.class, CloseableHttpClient.class, LoggingService.class, IOFogNetworkInterfaceManager.class,
        InetAddress.class, HttpGet.class, BufferedReader.class, InputStreamReader.class, CloseableHttpResponse.class,
        HttpEntity.class, InputStream.class, StatusLine.class, JsonReader.class, Json.class, File.class,
        FileInputStream.class, CertificateFactory.class, Certificate.class, CertificateFactorySpi.class, Provider.class,
        SSLContext.class, SSLConnectionSocketFactory.class, HttpClientBuilder.class, StringEntity.class, FieldAgent.class, MultipartEntityBuilder.class,
        TrustManagers.class, TrustManagerFactory.class})
@PowerMockIgnore({"java.net.ssl.*"})
public class OrchestratorTest {
    private Orchestrator orchestrator;
    private SSLConnectionSocketFactory sslConnectionSocketFactory;
    private CloseableHttpClient httpClients;
    private InetAddress inetAddress;
    private HttpGet httpGet;
    private BufferedReader bufferedReader;
    private InputStreamReader reader;
    private CloseableHttpResponse response;
    private HttpEntity httpEntity;
    private InputStream inputStream;
    private StatusLine statusLine;
    private JsonReader jsonReader;
    private JsonObject jsonObject;
    private JsonObject anotherJsonObject;
    private JsonObjectBuilder jsonObjectBuilder;
    private FileInputStream fileInputStream;
    private CertificateFactory certificateFactory;
    private Certificate certificate;
    private CertificateFactorySpi certificateFactorySpi;
    private Provider provider;
    private SSLContext sslContext;
    private HttpClientBuilder httpClientBuilder;
    private StringEntity stringEntity;
    private FieldAgent fieldAgent;
    private String provisionKey;
    private File file;
    private MultipartEntityBuilder multipartEntityBuilder;
    private IOFogNetworkInterfaceManager iOFogNetworkInterfaceManager;
    TrustManagerFactory trustManagerFactory;

    @Before
    public void setUp() throws Exception {
        provisionKey = "provisionKey";
        mockStatic(Configuration.class);
        mockStatic(LoggingService.class);
        mockStatic(HttpClients.class);
        mockStatic(LoggingService.class);
        mockStatic(Json.class);
        mockStatic(CertificateFactory.class);
        mockStatic(SSLContext.class);
        mockStatic(FieldAgent.class);
        mockStatic(MultipartEntityBuilder.class);
        mockStatic(IOFogNetworkInterfaceManager.class);
        sslConnectionSocketFactory = mock(SSLConnectionSocketFactory.class);
        file = mock(File.class);
        multipartEntityBuilder = mock(MultipartEntityBuilder.class);
        httpClients = mock(CloseableHttpClient.class);
        inetAddress = mock(InetAddress.class);
        httpGet = mock(HttpGet.class);
        bufferedReader = mock(BufferedReader.class);
        reader = mock(InputStreamReader.class);
        response = mock(CloseableHttpResponse.class);
        httpEntity = mock(HttpEntity.class);
        inputStream = mock(InputStream.class);
        statusLine = mock(StatusLine.class);
        jsonReader = mock(JsonReader.class);
        jsonObject = mock(JsonObject.class);
        anotherJsonObject = mock(JsonObject.class);
        jsonObjectBuilder = mock(JsonObjectBuilder.class);
        fileInputStream = mock(FileInputStream.class);
        certificate = PowerMockito.mock(Certificate.class);
        certificateFactorySpi = mock(CertificateFactorySpi.class);
        provider = mock(Provider.class);
        certificateFactory = PowerMockito.mock(CertificateFactory.class);
        sslContext = PowerMockito.mock(SSLContext.class);
        httpClientBuilder = mock(HttpClientBuilder.class);
        stringEntity = Mockito.mock(StringEntity.class);
        fieldAgent = PowerMockito.mock(FieldAgent.class);
        iOFogNetworkInterfaceManager = PowerMockito.mock(IOFogNetworkInterfaceManager.class);
        PowerMockito.when(file.getName()).thenReturn("fileName");
        PowerMockito.when(FieldAgent.getInstance()).thenReturn(fieldAgent);
        PowerMockito.when(fieldAgent.deProvision(Mockito.anyBoolean())).thenReturn("success");
        PowerMockito.when(MultipartEntityBuilder.create()).thenReturn(multipartEntityBuilder);
        PowerMockito.when(multipartEntityBuilder.build()).thenReturn(httpEntity);
        PowerMockito.when(Configuration.getIofogUuid()).thenReturn("iofog-uuid");
        PowerMockito.when(Configuration.getFogType()).thenReturn(ArchitectureType.ARM);
        PowerMockito.when(Configuration.getAccessToken()).thenReturn("access-token");
        PowerMockito.when(Configuration.getControllerUrl()).thenReturn("http://controller/");
        PowerMockito.when(Configuration.isSecureMode()).thenReturn(false);
        PowerMockito.when(Configuration.getControllerCert()).thenReturn("controllerCert");
        PowerMockito.when(IOFogNetworkInterfaceManager.getInstance()).thenReturn(iOFogNetworkInterfaceManager);
        PowerMockito.when(iOFogNetworkInterfaceManager.getInetAddress()).thenReturn(inetAddress);
        PowerMockito.whenNew(SSLConnectionSocketFactory.class)
                .withParameterTypes(SSLContext.class)
                .withArguments(Mockito.any()).thenReturn(sslConnectionSocketFactory);
        PowerMockito.when(HttpClients.createDefault()).thenReturn(httpClients);
        PowerMockito.when(httpClients.execute(Mockito.any())).thenReturn(response);
        PowerMockito.when(response.getEntity()).thenReturn(httpEntity);
        PowerMockito.when(response.getStatusLine()).thenReturn(statusLine);
        PowerMockito.when(statusLine.getStatusCode()).thenReturn(200);
        PowerMockito.when(httpEntity.getContent()).thenReturn(inputStream);
        PowerMockito.whenNew(HttpGet.class)
                .withParameterTypes(String.class)
                .withArguments(Mockito.any()).thenReturn(httpGet);
        PowerMockito.whenNew(BufferedReader.class)
                .withParameterTypes(Reader.class)
                .withArguments(Mockito.any())
                .thenReturn(bufferedReader);
        PowerMockito.whenNew(InputStreamReader.class)
                .withParameterTypes(InputStream.class, String.class)
                .withArguments(Mockito.any(), Mockito.anyString())
                .thenReturn(reader);
        PowerMockito.whenNew(FileInputStream.class)
                .withParameterTypes(String.class)
                .withArguments(Mockito.anyString())
                .thenReturn(fileInputStream);
        PowerMockito.whenNew(FileInputStream.class)
                .withParameterTypes(File.class)
                .withArguments(Mockito.any())
                .thenReturn(fileInputStream);
        PowerMockito.when(Json.createReader(Mockito.any(Reader.class))).thenReturn(jsonReader);
        PowerMockito.when(Json.createObjectBuilder()).thenReturn(jsonObjectBuilder);
        PowerMockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyString())).thenReturn(jsonObjectBuilder);
        PowerMockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyInt())).thenReturn(jsonObjectBuilder);
        PowerMockito.when(jsonObjectBuilder.build()).thenReturn(anotherJsonObject);
        PowerMockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        PowerMockito.when(SSLContext.getInstance(Mockito.anyString())).thenReturn(sslContext);
        PowerMockito.doNothing().when(sslContext).init(Mockito.any(KeyManager[].class),
                Mockito.any(TrustManager[].class), Mockito.any(SecureRandom.class));
        PowerMockito.when(HttpClients.custom()).thenReturn(httpClientBuilder);
        PowerMockito.when(httpClientBuilder.build()).thenReturn(httpClients);
        PowerMockito.when(CertificateFactory.getInstance(Mockito.any())).thenReturn(certificateFactory);
        PowerMockito.when(certificateFactory.generateCertificate(Mockito.any(InputStream.class))).thenReturn(certificate);
        PowerMockito.whenNew(StringEntity.class).withParameterTypes(String.class, ContentType.class)
                .withArguments(Mockito.anyString(), Mockito.eq(ContentType.APPLICATION_JSON))
                .thenReturn(stringEntity);
        PowerMockito.mock(TrustManagers.class);
        mockStatic(TrustManagers.class);
        trustManagerFactory = PowerMockito.mock(TrustManagerFactory.class);
        mockStatic(TrustManagerFactory.class);
        PowerMockito.when(TrustManagerFactory.getInstance(anyString())).thenReturn(trustManagerFactory);
        orchestrator = spy(new Orchestrator());
    }

    @After
    public void tearDown() throws Exception {
        provisionKey = null;
        orchestrator = null;
        Mockito.reset(certificateFactory, httpClientBuilder, jsonObjectBuilder, jsonReader, fileInputStream,
                stringEntity, response, anotherJsonObject, jsonObject);
    }

    /**
     * Test ping true
     */
    @Test
    public void testPingSuccess() {
        try {
            assertTrue(orchestrator.ping());
            PowerMockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test ping false
     */
    @Test
    public void testPingFailure() {
        try {
            PowerMockito.when(jsonObject.isNull("status")).thenReturn(true);
            assertFalse(orchestrator.ping());
            PowerMockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test ping throws Exception
     */
    @Test (expected = Exception.class)
    public void throwsExceptionWhenPingIsCalled() throws Exception{
        PowerMockito.when(jsonReader.readObject()).thenReturn(null);
        assertFalse(orchestrator.ping());
    }

    /**
     * Test ping When Controller url is https & secureMode
     */
    @Test (expected = AgentUserException.class)
    public void testPingWhenControllerUrlIsHttpsAndDevMode() throws Exception{
        PowerMockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
        PowerMockito.when(Configuration.isSecureMode()).thenReturn(true);
        assertFalse(orchestrator.ping());
    }

    /**
     * Test ping When response code is 400
     */
    @Test (expected = AgentUserException.class)
    public void throwsExceptionWhenResponseCodeIsNotOkOnPing() throws Exception{
        PowerMockito.when(statusLine.getStatusCode()).thenReturn(400);
        assertFalse(orchestrator.ping());
    }

    /**
     * Test ping When response code is 404
     */
    @Test (expected = AgentUserException.class)
    public void throwsExceptionWhenResponseCodeIs404OnPing() throws Exception{
        PowerMockito.when(statusLine.getStatusCode()).thenReturn(404);
        assertFalse(orchestrator.ping());
    }
    /**
     * Test ping When InputStream throws Exception
     */
    @Test (expected = AgentUserException.class)
    public void throwsUnsupportedEncodingExceptionWhenInputStreamIsCreatedInPing() throws Exception{
        PowerMockito.whenNew(InputStreamReader.class)
                .withParameterTypes(InputStream.class, String.class)
                .withArguments(Mockito.any(), Mockito.anyString())
                .thenThrow(mock(UnsupportedEncodingException.class));
        orchestrator.ping();
        PowerMockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
    }
    /**
     * Test ping When client throws ClientProtocolException
     */
    @Test (expected = AgentUserException.class)
    public void throwsClientProtocolExceptionWhenHttpsClientExecuteIsCalledInPing() throws Exception{
        PowerMockito.doThrow(mock(ClientProtocolException.class)).when(httpClients).execute(Mockito.any());
        assertFalse(orchestrator.ping());
        PowerMockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
    }

    /**
     * Test ping When client throws IOException
     */
    @Test (expected = AgentUserException.class)
    public void throwsIOExceptionWhenHttpsClientExecuteIsCalledInPing() throws Exception{
        PowerMockito.doThrow(mock(IOException.class)).when(httpClients).execute(Mockito.any());
        assertFalse(orchestrator.ping());
        PowerMockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
    }

    /**
     * Test ping When client throws IOException
     */
    @Test (expected = AgentUserException.class)
    public void throwsExceptionWhenResponseIsNullCalledInPing() throws Exception{
        PowerMockito.when(httpClients.execute(Mockito.any())).thenReturn(null);
        assertFalse(orchestrator.ping());
        PowerMockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
    }

    /**
     * Test ping When client throws IOException
     */
    @Test (expected = AgentUserException.class)
    public void throwsUnsupportedOperationExceptionWhenResponseContentIsCalledInPing() throws Exception{
        PowerMockito.doThrow(mock(UnsupportedOperationException.class)).when(httpEntity).getContent();
        assertFalse(orchestrator.ping());
        PowerMockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
    }

    /**
     * Test request when json is null
     */
    @Test
    public void testRequest() throws Exception {
        JsonObject jsonResponse = orchestrator.request("deprovision", RequestType.POST, null, null);
        assertEquals(jsonObject, jsonResponse);
        PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                Mockito.eq(RequestType.POST), Mockito.eq(stringEntity),  Mockito.any());
    }

    /**
     * Test request when json is not null & command is blank
     */
    @Test
    public void testRequestWhenCommandIsBlank() {
        JsonObject jsonResponse = null;
        try {
            jsonResponse = orchestrator.request("", RequestType.PATCH, null, jsonObject);
            assertEquals(jsonObject, jsonResponse);
            PowerMockito.verifyStatic(Configuration.class);
            Configuration.isSecureMode();
            PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                    Mockito.eq(RequestType.PATCH), Mockito.eq(stringEntity),  Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test request when devMode is true
     */
    @Test (expected = AgentUserException.class)
    public void throwsAgentUserExceptionWhenDevModeIsTrue() throws Exception {
        PowerMockito.when(Configuration.isSecureMode()).thenReturn(true);
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertEquals(jsonObject, jsonResponse);
        PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq("delete"),
                Mockito.eq(RequestType.DELETE), Mockito.eq(null),  Mockito.eq(null));
    }

    /**
     * Test request command is delete & responseCode is 204
     */
    @Test
    public void testWhenCommandIsDelete() throws Exception {
        PowerMockito.when(statusLine.getStatusCode()).thenReturn(204);
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        PowerMockito.verifyStatic(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
        PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
    }

    /**
     * Test request command is delete & responseCode is 400
     */
    @Test (expected = BadRequestException.class)
    public void throwsBadRequestExceptionWhenCommandIsDelete() throws Exception {
        PowerMockito.when(statusLine.getStatusCode()).thenReturn(400);
        PowerMockito.when(jsonObject.getString("message")).thenReturn("Error");
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        PowerMockito.verifyStatic(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
        PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }
    /**
     * Test request command is delete & responseCode is 401
     */
    @Test (expected = AuthenticationException.class)
    public void throwsAuthenticationExceptionWhenCommandIsDelete() throws Exception {
        PowerMockito.when(statusLine.getStatusCode()).thenReturn(401);
        PowerMockito.when(jsonObject.getString("message")).thenReturn("Error");
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        PowerMockito.verifyStatic(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
        PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }

    /**
     * Test request command is delete & responseCode is 403
     */
    @Test (expected = ForbiddenException.class)
    public void throwsForbiddenExceptionWhenCommandIsDelete() throws Exception {
        PowerMockito.when(statusLine.getStatusCode()).thenReturn(403);
        PowerMockito.when(jsonObject.getString("message")).thenReturn("Error");
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        PowerMockito.verifyStatic(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
        PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }

    /**
     * Test request command is delete & responseCode is 404
     */
    @Test (expected = NotFoundException.class)
    public void throwsNotFoundExceptionWhenCommandIsDelete() throws Exception {
        PowerMockito.when(statusLine.getStatusCode()).thenReturn(404);
        PowerMockito.when(jsonObject.getString("message")).thenReturn("Error");
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        PowerMockito.verifyStatic(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
        PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }

    /**
     * Test request command is delete & responseCode is 404
     */
    @Test (expected = InternalServerErrorException.class)
    public void throwsInternalServerErrorExceptionWhenCommandIsDelete() throws Exception {
        PowerMockito.when(statusLine.getStatusCode()).thenReturn(500);
        PowerMockito.when(jsonObject.getString("message")).thenReturn("Error");
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        PowerMockito.verifyStatic(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
        PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }
    /**
     * Test request command is delete & InputStreamReader throws UnsupportedEncodingException
     */
    @Test (expected = AgentUserException.class)
    public void throwsUnsupportedEncodingExceptionWhenInputStreamReaderIsCreated() throws Exception {
        PowerMockito.whenNew(InputStreamReader.class)
                .withParameterTypes(InputStream.class, String.class)
                .withArguments(Mockito.any(), Mockito.anyString())
                .thenThrow(mock(UnsupportedEncodingException.class));
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.GET, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        PowerMockito.verifyStatic(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
        PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                Mockito.eq(RequestType.GET), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }

    /**
     * Test request
     */
    @Test
    public void testRequestWhenCommandIsStrace() {
        JsonObject jsonResponse = null;
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("key", "value");
        try {
            jsonResponse = orchestrator.request("strace", RequestType.PUT, queryParams, jsonObject);
            assertEquals(jsonObject, jsonResponse);
            PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(queryParams),
                    Mockito.eq(RequestType.PUT), Mockito.eq(stringEntity),  Mockito.any());
            PowerMockito.verifyStatic(Configuration.class);
            Configuration.isSecureMode();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test request when url is https
     */
    @Test
    public void testRequestWhenControllerUrlIsHttps() {
        JsonObject jsonResponse = null;
        try {
            PowerMockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
            orchestrator = spy(new Orchestrator());
            jsonResponse = orchestrator.request("strace", RequestType.PUT, null, jsonObject);
            assertEquals(jsonObject, jsonResponse);
            PowerMockito.verifyPrivate(orchestrator, Mockito.atLeastOnce()).invoke("getJsonObject", Mockito.eq(null),
                    Mockito.eq(RequestType.PUT), Mockito.eq(stringEntity),  Mockito.any());
            PowerMockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(true));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test provision
     */
    @Test
    public void testProvisionSuccess() {
        try {
            JsonObject jsonResponse = orchestrator.provision(provisionKey);
            assertEquals(jsonObject, jsonResponse);
            Mockito.verify(orchestrator).request(Mockito.eq("provision"),
                    Mockito.eq(RequestType.POST), Mockito.eq(null), Mockito.eq(anotherJsonObject));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test provision throws AgentSystemException
     */
    @Test (expected = AgentSystemException.class)
    public void throwsAgentSystemExceptionOnProvision() throws Exception{
        PowerMockito.doThrow(mock(Exception.class)).when(orchestrator).request(Mockito.eq("provision"),
                Mockito.eq(RequestType.POST), Mockito.eq(null), Mockito.eq(anotherJsonObject));
        JsonObject jsonResponse = orchestrator.provision(provisionKey);
        assertEquals(jsonObject, jsonResponse);
        Mockito.verify(orchestrator).request(Mockito.eq("provision"),
                Mockito.eq(RequestType.POST), Mockito.eq(null), Mockito.eq(anotherJsonObject));
    }

    /**
     * Test sendFileToController
     */
    @Test
    public void testSendFileToController() {
        try {
            orchestrator.sendFileToController("strace", file);
            PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                    Mockito.eq(RequestType.PUT), Mockito.eq(httpEntity),  Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test sendFileToController
     */
    @Test (expected = Exception.class)
    public void throwsExceptionSendFileToController() throws Exception{
        PowerMockito.doThrow(mock(Exception.class)).when(httpClients).execute(Mockito.any());
        orchestrator.sendFileToController("strace", file);
        PowerMockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
                Mockito.eq(RequestType.PUT), Mockito.eq(httpEntity),  Mockito.any());
    }

    /**
     * Test update when controller url is http
     */
    @Test
    public void testUpdateWhenControllerUrlIsHttp() {
        try {
            orchestrator.update();
            PowerMockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(false));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test update when FileInputStream throws IOException
     */
    @Test
    public void testUpdateFileInputStreamThrowsException() {
        try {
            PowerMockito.whenNew(FileInputStream.class)
                    .withParameterTypes(String.class)
                    .withArguments(Mockito.anyString())
                    .thenThrow(mock(IOException.class));
            PowerMockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
            orchestrator.update();
            PowerMockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(true));
            PowerMockito.verifyPrivate(orchestrator, Mockito.never()).invoke("getCert", Mockito.eq(fileInputStream));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test update when FileInputStream throws IOException
     */
    @Test
    public void testUpdateWhenGetCertThrowsException() {
        try {
            PowerMockito.when(certificateFactory.generateCertificate(Mockito.any(InputStream.class))).thenThrow(mock(CertificateException.class));
            PowerMockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
            orchestrator.update();
            PowerMockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(true));
            PowerMockito.verifyPrivate(orchestrator).invoke("getCert", Mockito.eq(fileInputStream));
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logError(Mockito.eq("Orchestrator"),
                    Mockito.eq("unable to get certificate"), Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test update when initialize throws AgentException
     */
    @Test
    public void testUpdateWhenInitializeThrowsException() {
        try {
            orchestrator = spy(new Orchestrator());
            PowerMockito.when(SSLContext.getInstance(Mockito.anyString())).thenThrow(mock(NoSuchAlgorithmException.class));
            PowerMockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
            orchestrator.update();
            PowerMockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(true));
            PowerMockito.verifyPrivate(orchestrator).invoke("getCert", Mockito.eq(fileInputStream));
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logError(Mockito.eq("Orchestrator"),
                    Mockito.eq("Error while updating local variables when changes applied"), Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test update when initialize throws AgentException
     */
    @Test
    public void testUpdateWhenInitializeThrowsKeyManagementException() {
        try {
            // orchestrator = spy(new Orchestrator());
            PowerMockito.doThrow(mock(KeyManagementException.class)).when(sslContext).init(Mockito.eq(null),
                    Mockito.any(), Mockito.any(SecureRandom.class));
            PowerMockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
            orchestrator.update();
            PowerMockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(true));
            PowerMockito.verifyPrivate(orchestrator).invoke("getCert", Mockito.eq(fileInputStream));
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logError(Mockito.eq("Orchestrator"),
                    Mockito.eq("Error while updating local variables when changes applied"), Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}