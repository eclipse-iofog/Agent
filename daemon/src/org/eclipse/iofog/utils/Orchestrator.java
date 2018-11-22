/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.eclipse.iofog.field_agent.enums.RequestType;
import org.eclipse.iofog.network.IOFogNetworkInterface;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.trustmanager.X509TrustManagerImpl;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.naming.AuthenticationException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.io.*;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Map;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

/**
 * provides methods for IOFog controller
 *
 * @author saeid
 */
public class Orchestrator {
    private static final int CONNECTION_TIMEOUT = 5000;
    private String controllerUrl;
    private String iofogUuid;
    private String iofogAccessToken;
    private Certificate controllerCert;
    private CloseableHttpClient client;

    private static final String MODULE_NAME = "Orchestrator";

    public Orchestrator() {
        this.update();
    }

    /**
     * ping IOFog controller
     *
     * @return ping result
     * @throws Exception
     */
    public boolean ping() throws Exception {
        try {
            JsonObject result = getJSON(controllerUrl + "status");
            return result.getString("status").equals("online");
        } catch (Exception exp) {
            logWarning(MODULE_NAME, exp.getMessage());
            throw exp;
        }
    }

    /**
     * does provisioning
     *
     * @param key - provisioning key
     * @return result in Json format
     * @throws Exception
     */
    public JsonObject provision(String key) throws Exception {
        JsonObject result;
        JsonObject json = Json.createObjectBuilder()
                .add("key", key)
                .add("type", Configuration.getFogType().getCode())
                .build();

        result = request("provision", RequestType.POST, null, json);
        return result;
    }

    private RequestConfig getRequestConfig() throws Exception {
        return RequestConfig.copy(RequestConfig.DEFAULT)
                .setLocalAddress(IOFogNetworkInterface.getInetAddress())
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .build();
    }

    /**
     * initialize {@link TrustManager}
     *
     * @throws Exception
     */
    private void initialize(boolean secure) throws Exception {
        if (secure) {
            TrustManager[] trustManager = new TrustManager[]{new X509TrustManagerImpl(controllerCert)};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManager, new SecureRandom());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
            client = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        } else {
            client = HttpClients.createDefault();
        }
    }

    /**
     * converts {@link InputStream} to {@link Certificate}
     *
     * @param is - {@link InputStream}
     * @return {@link Certificate}
     */
    private Certificate getCert(InputStream is) {
        Certificate result = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            result = certificateFactory.generateCertificate(is);
        } catch (CertificateException exp) {
            logWarning(MODULE_NAME, exp.getMessage());
        }
        return result;
    }

    /**
     * gets Json result of a IOFog Controller endpoint
     *
     * @param surl - endpoind to be called
     * @return result in Json format
     * @throws Exception
     */
    private JsonObject getJSON(String surl) throws Exception {
        // disable certificates for dev mode
        boolean secure = true;
        if (!surl.toLowerCase().startsWith("https")) {
            if (!Configuration.isDeveloperMode())
                throw new UnknownHostException("unable to connect over non-secure connection");
            else
                secure = false;
        }
        initialize(secure);
        RequestConfig config = getRequestConfig();
        HttpGet get = new HttpGet(surl);
        get.setConfig(config);

        JsonObject result;

        try (CloseableHttpResponse response = client.execute(get)) {

            if (response.getStatusLine().getStatusCode() != 200) {
                if (response.getStatusLine().getStatusCode() == 404)
                    throw new UnknownHostException();
                else
                    throw new Exception();
            }

            try (Reader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                 JsonReader jsonReader = Json.createReader(in)) {
                result = jsonReader.readObject();
            }

        }
        return result;
    }

    public JsonObject request(String command, RequestType requestType, Map<String, Object> queryParams, JsonObject json) throws Exception {
        if (json == null) {
            json = Json.createObjectBuilder().build();
        }

        return getJsonObject(queryParams, requestType, new StringEntity(json.toString(), ContentType.APPLICATION_JSON), createUri(command));
    }

    private StringBuilder createUri(String command) {
        StringBuilder uri = new StringBuilder(controllerUrl);
        uri.append("agent/")
                .append(command);
        return uri;
    }


    private JsonObject getJsonObject(Map<String, Object> queryParams, RequestType requestType, HttpEntity httpEntity, StringBuilder uri) throws Exception {
        // disable certificates for dev mode
        boolean secure = true;
        if (!controllerUrl.toLowerCase().startsWith("https")) {
            if (!Configuration.isDeveloperMode())
                throw new UnknownHostException("unable to connect over non-secure connection");
            else
                secure = false;
        }

        JsonObject result = Json.createObjectBuilder().build();

        if (queryParams != null)
            queryParams.forEach((key, value) -> uri.append("/").append(key)
                    .append("/").append(value));

        initialize(secure);
        HttpRequestBase req;

        RequestConfig config = getRequestConfig();

        switch (requestType) {
            case GET:
                req = new HttpGet(uri.toString());
                break;
            case POST:
                req = new HttpPost(uri.toString());
                ((HttpPost) req).setEntity(httpEntity);
                break;
            case PUT:
                req = new HttpPut(uri.toString());
                ((HttpPut) req).setEntity(httpEntity);
                break;
            case PATCH:
                req = new HttpPatch(uri.toString());
                ((HttpPatch) req).setEntity(httpEntity);
                break;
            case DELETE:
                req = new HttpDelete(uri.toString());
                break;
            default:
                req = new HttpGet(uri.toString());
                break;
        }

        req.setConfig(config);

        String token = Configuration.getAccessToken();
        if (!StringUtils.isEmpty(token)) {
            req.addHeader(new BasicHeader("Authorization", token));
        }

        try (CloseableHttpResponse response = client.execute(req)) {
            String errorMessage = "";
            if (response.getEntity() != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                JsonReader jsonReader = Json.createReader(in);

                result = jsonReader.readObject();
                errorMessage = result.getString("message", "");
            }


            switch (response.getStatusLine().getStatusCode()) {
                case 204:
                    return Json.createObjectBuilder().build();
                case 400:
                    throw new BadRequestException(errorMessage);
                case 401:
                    logWarning(MODULE_NAME, "Invalid authentication ioFog token, switching controller state to broken");
                    StatusReporter.setFieldAgentStatus().setControllerStatus(Constants.ControllerStatus.BROKEN);
                    throw new AuthenticationException(errorMessage);
                case 403:
                    throw new ForbiddenException(errorMessage);
                case 404:
                    throw new NotFoundException(errorMessage);
                case 500:
                    throw new InternalServerErrorException(errorMessage);
            }

        } catch (UnsupportedEncodingException exp) {
            logWarning(MODULE_NAME, exp.getMessage());
            throw exp;
        }

        return result;
    }

    /**
     * calls IOFog Controller endpoind to send file and returns Json result
     *
     * @param command - endpoint to be called
     * @param file    - file to send
     * @return result in Json format
     * @throws Exception
     */
    public void sendFileToController(String command, File file) throws Exception {
        InputStream inputStream = new FileInputStream(file);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addBinaryBody("upstream", inputStream, ContentType.create("application/zip"), file.getName());

        HttpEntity entity = builder.build();

        getJsonObject(null, RequestType.PUT, entity, createUri(command));
    }

    /**
     * updates local variables when changes applied
     */
    public void update() {
        iofogUuid = Configuration.getIofogUuid();
        iofogAccessToken = Configuration.getAccessToken();
        controllerUrl = Configuration.getControllerUrl();
        // disable certificates for dev mode
        boolean secure = true;
        if (controllerUrl.toLowerCase().startsWith("https")) {
            try (FileInputStream fileInputStream = new FileInputStream(Configuration.getControllerCert())) {
                controllerCert = getCert(fileInputStream);
            } catch (IOException e) {
                controllerCert = null;
            }
        } else {
            controllerCert = null;
            secure = false;
        }
        try {
            initialize(secure);
        } catch (Exception exp) {
            logWarning(MODULE_NAME, exp.getMessage());
        }
    }
}
