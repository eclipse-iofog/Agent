/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2024 Edgeworx, Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Disabled
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
    private MockedStatic<Configuration> configurationMockedStatic;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<HttpClients> httpClientsMockedStatic;
    private MockedStatic<Json> jsonMockedStatic;
    private MockedStatic<CertificateFactory> certificateFactoryMockedStatic;
    private MockedStatic<SSLContext> sslContextMockedStatic;
    private MockedStatic<FieldAgent> fieldAgentMockedStatic;
    private MockedStatic<MultipartEntityBuilder> multipartEntityBuilderMockedStatic;
    private MockedStatic<IOFogNetworkInterfaceManager> ioFogNetworkInterfaceManagerMockedStatic;
    private MockedStatic<TrustManagers> trustManagersMockedStatic;
    private MockedStatic<TrustManagerFactory> trustManagerFactoryMockedStatic;
    private MockedConstruction<SSLConnectionSocketFactory> sslConnectionSocketFactoryMockedConstruction;
    private MockedConstruction<HttpGet> httpGetMockedConstruction;
    private MockedConstruction<BufferedReader> bufferedReaderMockedConstruction;
    private MockedConstruction<InputStreamReader> inputStreamReaderMockedConstruction;
    private MockedConstruction<FileInputStream> fileInputStreamMockedConstruction;
    private MockedConstruction<StringEntity> stringEntityMockedConstruction;


    @BeforeEach
    public void setUp() throws Exception {
        provisionKey = "provisionKey";
        configurationMockedStatic = mockStatic(Configuration.class);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        httpClientsMockedStatic = mockStatic(HttpClients.class);
        jsonMockedStatic = mockStatic(Json.class);
        certificateFactoryMockedStatic = mockStatic(CertificateFactory.class);
        sslContextMockedStatic = mockStatic(SSLContext.class);
        fieldAgentMockedStatic = mockStatic(FieldAgent.class);
        multipartEntityBuilderMockedStatic = mockStatic(MultipartEntityBuilder.class);
        ioFogNetworkInterfaceManagerMockedStatic = mockStatic(IOFogNetworkInterfaceManager.class);
        trustManagersMockedStatic = mockStatic(TrustManagers.class);
        trustManagerFactoryMockedStatic = mockStatic(TrustManagerFactory.class);
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
        certificate = Mockito.mock(Certificate.class);
        certificateFactorySpi = mock(CertificateFactorySpi.class);
        provider = mock(Provider.class);
        certificateFactory = Mockito.mock(CertificateFactory.class);
        sslContext = Mockito.mock(SSLContext.class);
        httpClientBuilder = mock(HttpClientBuilder.class);
        stringEntity = Mockito.mock(StringEntity.class);
        fieldAgent = Mockito.mock(FieldAgent.class);
        iOFogNetworkInterfaceManager = Mockito.mock(IOFogNetworkInterfaceManager.class);
        Mockito.when(file.getName()).thenReturn("fileName");
        Mockito.when(FieldAgent.getInstance()).thenReturn(fieldAgent);
        Mockito.when(fieldAgent.deProvision(Mockito.anyBoolean())).thenReturn("success");
        Mockito.when(MultipartEntityBuilder.create()).thenReturn(multipartEntityBuilder);
        Mockito.when(multipartEntityBuilder.build()).thenReturn(httpEntity);
        Mockito.when(Configuration.getIofogUuid()).thenReturn("iofog-uuid");
        Mockito.when(Configuration.getFogType()).thenReturn(ArchitectureType.ARM);
        Mockito.when(Configuration.getAccessToken()).thenReturn("access-token");
        Mockito.when(Configuration.getControllerUrl()).thenReturn("http://controller/");
        Mockito.when(Configuration.isSecureMode()).thenReturn(false);
        Mockito.when(Configuration.getControllerCert()).thenReturn("controllerCert");
        Mockito.when(IOFogNetworkInterfaceManager.getInstance()).thenReturn(iOFogNetworkInterfaceManager);
        Mockito.when(iOFogNetworkInterfaceManager.getInetAddress()).thenReturn(inetAddress);
        sslConnectionSocketFactoryMockedConstruction = Mockito.mockConstruction(SSLConnectionSocketFactory.class);
//        Mockito.whenNew(SSLConnectionSocketFactory.class)
//                .withParameterTypes(SSLContext.class)
//                .withArguments(Mockito.any()).thenReturn(sslConnectionSocketFactory);
        Mockito.when(HttpClients.createDefault()).thenReturn(httpClients);
        Mockito.when(httpClients.execute(Mockito.any())).thenReturn(response);
        Mockito.when(response.getEntity()).thenReturn(httpEntity);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContent()).thenReturn(inputStream);
        httpGetMockedConstruction = Mockito.mockConstruction(HttpGet.class);
        bufferedReaderMockedConstruction = Mockito.mockConstruction(BufferedReader.class);
        inputStreamReaderMockedConstruction = Mockito.mockConstruction(InputStreamReader.class);
        fileInputStreamMockedConstruction = Mockito.mockConstruction(FileInputStream.class);
//        Mockito.whenNew(HttpGet.class)
//                .withParameterTypes(String.class)
//                .withArguments(Mockito.any()).thenReturn(httpGet);
//        Mockito.whenNew(BufferedReader.class)
//                .withParameterTypes(Reader.class)
//                .withArguments(Mockito.any())
//                .thenReturn(bufferedReader);
//        Mockito.whenNew(InputStreamReader.class)
//                .withParameterTypes(InputStream.class, String.class)
//                .withArguments(Mockito.any(), Mockito.anyString())
//                .thenReturn(reader);
//        Mockito.whenNew(FileInputStream.class)
//                .withParameterTypes(String.class)
//                .withArguments(Mockito.anyString())
//                .thenReturn(fileInputStream);
//        Mockito.whenNew(FileInputStream.class)
//                .withParameterTypes(File.class)
//                .withArguments(Mockito.any())
//                .thenReturn(fileInputStream);
        Mockito.when(Json.createReader(Mockito.any(Reader.class))).thenReturn(jsonReader);
        Mockito.when(Json.createObjectBuilder()).thenReturn(jsonObjectBuilder);
        Mockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyString())).thenReturn(jsonObjectBuilder);
        Mockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyInt())).thenReturn(jsonObjectBuilder);
        Mockito.when(jsonObjectBuilder.build()).thenReturn(anotherJsonObject);
        Mockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        Mockito.when(SSLContext.getInstance(Mockito.anyString())).thenReturn(sslContext);
        Mockito.doNothing().when(sslContext).init(Mockito.any(KeyManager[].class),
                Mockito.any(TrustManager[].class), Mockito.any(SecureRandom.class));
        Mockito.when(HttpClients.custom()).thenReturn(httpClientBuilder);
        Mockito.when(httpClientBuilder.build()).thenReturn(httpClients);
        Mockito.when(CertificateFactory.getInstance(Mockito.any())).thenReturn(certificateFactory);
        Mockito.when(certificateFactory.generateCertificate(Mockito.any(InputStream.class))).thenReturn(certificate);
        stringEntityMockedConstruction = Mockito.mockConstruction(StringEntity.class);
//        Mockito.whenNew(StringEntity.class).withParameterTypes(String.class, ContentType.class)
//                .withArguments(Mockito.anyString(), Mockito.eq(ContentType.APPLICATION_JSON))
//                .thenReturn(stringEntity);
        Mockito.mock(TrustManagers.class);

        trustManagerFactory = Mockito.mock(TrustManagerFactory.class);
        Mockito.when(TrustManagerFactory.getInstance(anyString())).thenReturn(trustManagerFactory);
        orchestrator = spy(new Orchestrator());
    }

    @AfterEach
    public void tearDown() throws Exception {
        provisionKey = null;
        orchestrator = null;
        Mockito.reset(certificateFactory, httpClientBuilder, jsonObjectBuilder, jsonReader, fileInputStream,
                stringEntity, response, anotherJsonObject, jsonObject);
        configurationMockedStatic.close();
        loggingServiceMockedStatic.close();
        fieldAgentMockedStatic.close();
        httpClientsMockedStatic.close();
        sslContextMockedStatic.close();
        trustManagersMockedStatic.close();
        trustManagerFactoryMockedStatic.close();
        ioFogNetworkInterfaceManagerMockedStatic.close();
        certificateFactoryMockedStatic.close();
        jsonMockedStatic.close();
        multipartEntityBuilderMockedStatic.close();
    }

    /**
     * Test ping true
     */
    @Test
    public void testPingSuccess() {
        try {
            assertTrue(orchestrator.ping());
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
            Mockito.when(jsonObject.isNull("status")).thenReturn(true);
            assertFalse(orchestrator.ping());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test ping throws Exception
     */
    @Test
//            (expected = Exception.class)
    public void throwsExceptionWhenPingIsCalled() throws Exception{
        Mockito.when(jsonReader.readObject()).thenReturn(null);
        assertFalse(orchestrator.ping());
    }

    /**
     * Test ping When Controller url is https & secureMode
     */
    @Test
//            (expected = AgentUserException.class)
    public void testPingWhenControllerUrlIsHttpsAndDevMode() throws Exception{
        Mockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
        Mockito.when(Configuration.isSecureMode()).thenReturn(true);
        assertFalse(orchestrator.ping());
    }

    /**
     * Test ping When response code is 400
     */
    @Test
//            (expected = AgentUserException.class)
    public void throwsExceptionWhenResponseCodeIsNotOkOnPing() throws Exception{
        Mockito.when(statusLine.getStatusCode()).thenReturn(400);
        assertFalse(orchestrator.ping());
    }

    /**
     * Test ping When response code is 404
     */
    @Test
            // (expected = AgentUserException.class)
    public void throwsExceptionWhenResponseCodeIs404OnPing() throws Exception{
        Mockito.when(statusLine.getStatusCode()).thenReturn(404);
        assertFalse(orchestrator.ping());
    }
    /**
     * Test ping When InputStream throws Exception
     */
    @Test
            // (expected = AgentUserException.class)
    public void throwsUnsupportedEncodingExceptionWhenInputStreamIsCreatedInPing() throws Exception{
        inputStreamReaderMockedConstruction.close();
        inputStreamReaderMockedConstruction =
                Mockito.mockConstructionWithAnswer(InputStreamReader.class, invocation -> {
                    throw new UnsupportedEncodingException();
                });
//        inputStreamReaderMockedConstruction = Mockito.mockConstruction(InputStreamReader.class, (mock, context) ->{
//
//        } );
//
//        Mockito.whenNew(InputStreamReader.class)
//                .withParameterTypes(InputStream.class, String.class)
//                .withArguments(Mockito.any(), Mockito.anyString())
//                .thenThrow(mock(UnsupportedEncodingException.class));
        assertThrows(UnsupportedEncodingException.class, ()-> orchestrator.ping());
    }
    /**
     * Test ping When client throws ClientProtocolException
     */
    @Test
//            (expected = AgentUserException.class)
    public void throwsClientProtocolExceptionWhenHttpsClientExecuteIsCalledInPing() throws Exception{
        Mockito.doThrow(mock(ClientProtocolException.class)).when(httpClients).execute(Mockito.any());
        assertFalse(orchestrator.ping());
//        Mockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
    }

    /**
     * Test ping When client throws IOException
     */
    @Test
            //(expected = AgentUserException.class)
    public void throwsIOExceptionWhenHttpsClientExecuteIsCalledInPing() throws Exception{
        Mockito.doThrow(mock(IOException.class)).when(httpClients).execute(Mockito.any());
        assertFalse(orchestrator.ping());
//        Mockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
    }

    /**
     * Test ping When client throws IOException
     */
    @Test
//            (expected = AgentUserException.class)
    public void throwsExceptionWhenResponseIsNullCalledInPing() throws Exception{
        Mockito.when(httpClients.execute(Mockito.any())).thenReturn(null);
        assertFalse(orchestrator.ping());
//        Mockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
    }

    /**
     * Test ping When client throws IOException
     */
    @Test
//            (expected = AgentUserException.class)
    public void throwsUnsupportedOperationExceptionWhenResponseContentIsCalledInPing() throws Exception{
        Mockito.doThrow(mock(UnsupportedOperationException.class)).when(httpEntity).getContent();
        assertFalse(orchestrator.ping());
//        Mockito.verifyPrivate(orchestrator).invoke("getJSON", Mockito.eq("http://controller/status"));
    }

    /**
     * Test request when json is null
     */
    @Test
    public void testRequest() throws Exception {
        JsonObject jsonResponse = orchestrator.request("deprovision", RequestType.POST, null, null);
        assertEquals(jsonObject, jsonResponse);
//        Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                Mockito.eq(RequestType.POST), Mockito.eq(stringEntity),  Mockito.any());
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
            Mockito.verify(Configuration.class);
            Configuration.isSecureMode();
//            Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                    Mockito.eq(RequestType.PATCH), Mockito.eq(stringEntity),  Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test request when devMode is true
     */
    @Test
//            (expected = AgentUserException.class)
    public void throwsAgentUserExceptionWhenDevModeIsTrue() throws Exception {
        Mockito.when(Configuration.isSecureMode()).thenReturn(true);
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertEquals(jsonObject, jsonResponse);
//        Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq("delete"),
//                Mockito.eq(RequestType.DELETE), Mockito.eq(null),  Mockito.eq(null));
    }

    /**
     * Test request command is delete & responseCode is 204
     */
    @Test
    public void testWhenCommandIsDelete() throws Exception {
        Mockito.when(statusLine.getStatusCode()).thenReturn(204);
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        Mockito.verify(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
//        Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
    }

    /**
     * Test request command is delete & responseCode is 400
     */
    @Test
//            (expected = BadRequestException.class)
    public void throwsBadRequestExceptionWhenCommandIsDelete() throws Exception {
        Mockito.when(statusLine.getStatusCode()).thenReturn(400);
        Mockito.when(jsonObject.getString("message")).thenReturn("Error");
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        Mockito.verify(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
//        Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }
    /**
     * Test request command is delete & responseCode is 401
     */
    @Test
//            (expected = AuthenticationException.class)
    public void throwsAuthenticationExceptionWhenCommandIsDelete() throws Exception {
        Mockito.when(statusLine.getStatusCode()).thenReturn(401);
        Mockito.when(jsonObject.getString("message")).thenReturn("Error");
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        Mockito.verify(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
//        Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }

    /**
     * Test request command is delete & responseCode is 403
     */
    @Test
//            (expected = ForbiddenException.class)
    public void throwsForbiddenExceptionWhenCommandIsDelete() throws Exception {
        Mockito.when(statusLine.getStatusCode()).thenReturn(403);
        Mockito.when(jsonObject.getString("message")).thenReturn("Error");
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        Mockito.verify(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
//        Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }

    /**
     * Test request command is delete & responseCode is 404
     */
    @Test
//            (expected = NotFoundException.class)
    public void throwsNotFoundExceptionWhenCommandIsDelete() throws Exception {
        Mockito.when(statusLine.getStatusCode()).thenReturn(404);
        Mockito.when(jsonObject.getString("message")).thenReturn("Error");
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        Mockito.verify(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
//        Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }

    /**
     * Test request command is delete & responseCode is 404
     */
    @Test
//            (expected = InternalServerErrorException.class)
    public void throwsInternalServerErrorExceptionWhenCommandIsDelete() throws Exception {
        Mockito.when(statusLine.getStatusCode()).thenReturn(500);
        Mockito.when(jsonObject.getString("message")).thenReturn("Error");
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.DELETE, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        Mockito.verify(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
//        Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                Mockito.eq(RequestType.DELETE), Mockito.eq(stringEntity),  Mockito.any());
        Mockito.verify(fieldAgent).deProvision(Mockito.eq(true));
    }
    /**
     * Test request command is delete & InputStreamReader throws UnsupportedEncodingException
     */
    @Test
//            (expected = AgentUserException.class)
    public void throwsUnsupportedEncodingExceptionWhenInputStreamReaderIsCreated() throws Exception {
        inputStreamReaderMockedConstruction =
                Mockito.mockConstructionWithAnswer(InputStreamReader.class, invocation -> {
                    throw new UnsupportedEncodingException();
                });
//        Mockito.whenNew(InputStreamReader.class)
//                .withParameterTypes(InputStream.class, String.class)
//                .withArguments(Mockito.any(), Mockito.anyString())
//                .thenThrow(mock(UnsupportedEncodingException.class));
        JsonObject jsonResponse = orchestrator.request("delete", RequestType.GET, null, null);
        assertNotEquals(jsonObject, jsonResponse);
        Mockito.verify(Json.class, Mockito.atLeastOnce());
        Json.createObjectBuilder();
//        Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                Mockito.eq(RequestType.GET), Mockito.eq(stringEntity),  Mockito.any());
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
//            Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(queryParams),
//                    Mockito.eq(RequestType.PUT), Mockito.eq(stringEntity),  Mockito.any());
            Mockito.verify(Configuration.class);
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
            Mockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
            orchestrator = spy(new Orchestrator());
            jsonResponse = orchestrator.request("strace", RequestType.PUT, null, jsonObject);
            assertEquals(jsonObject, jsonResponse);
//            Mockito.verifyPrivate(orchestrator, Mockito.atLeastOnce()).invoke("getJsonObject", Mockito.eq(null),
//                    Mockito.eq(RequestType.PUT), Mockito.eq(stringEntity),  Mockito.any());
//            Mockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(true));
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
    @Test
//            (expected = AgentSystemException.class)
    public void throwsAgentSystemExceptionOnProvision() throws Exception{
        Mockito.doThrow(mock(Exception.class)).when(orchestrator).request(Mockito.eq("provision"),
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
//            Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                    Mockito.eq(RequestType.PUT), Mockito.eq(httpEntity),  Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test sendFileToController
     */
    @Test
//            (expected = Exception.class)
    public void throwsExceptionSendFileToController() throws Exception{
        Mockito.doThrow(mock(Exception.class)).when(httpClients).execute(Mockito.any());
        orchestrator.sendFileToController("strace", file);
//        Mockito.verifyPrivate(orchestrator).invoke("getJsonObject", Mockito.eq(null),
//                Mockito.eq(RequestType.PUT), Mockito.eq(httpEntity),  Mockito.any());
    }

    /**
     * Test update when controller url is http
     */
    @Test
    public void testUpdateWhenControllerUrlIsHttp() {
        try {
            orchestrator.update();
//            Mockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(false));
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
            fileInputStreamMockedConstruction.close();
            fileInputStreamMockedConstruction =
                    Mockito.mockConstructionWithAnswer(FileInputStream.class, invocation -> {
                        throw new IOException();
                    });
//            Mockito.whenNew(FileInputStream.class)
//                    .withParameterTypes(String.class)
//                    .withArguments(Mockito.anyString())
//                    .thenThrow(mock(IOException.class));
            Mockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
            orchestrator.update();
//            Mockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(true));
//            Mockito.verifyPrivate(orchestrator, Mockito.never()).invoke("getCert", Mockito.eq(fileInputStream));
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
            Mockito.when(certificateFactory.generateCertificate(Mockito.any(InputStream.class))).thenThrow(mock(CertificateException.class));
            Mockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
            orchestrator.update();
//            Mockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(true));
//            Mockito.verifyPrivate(orchestrator).invoke("getCert", Mockito.eq(fileInputStream));
            Mockito.verify(LoggingService.class);
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
            Mockito.when(SSLContext.getInstance(Mockito.anyString())).thenThrow(mock(NoSuchAlgorithmException.class));
            Mockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
            orchestrator.update();
//            Mockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(true));
//            Mockito.verifyPrivate(orchestrator).invoke("getCert", Mockito.eq(fileInputStream));
            Mockito.verify(LoggingService.class);
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
            Mockito.doThrow(mock(KeyManagementException.class)).when(sslContext).init(Mockito.eq(null),
                    Mockito.any(), Mockito.any(SecureRandom.class));
            Mockito.when(Configuration.getControllerUrl()).thenReturn("https://controller/");
            orchestrator.update();
//            Mockito.verifyPrivate(orchestrator).invoke("initialize", Mockito.eq(true));
//            Mockito.verifyPrivate(orchestrator).invoke("getCert", Mockito.eq(fileInputStream));
            Mockito.verify(LoggingService.class);
            LoggingService.logError(Mockito.eq("Orchestrator"),
                    Mockito.eq("Error while updating local variables when changes applied"), Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}