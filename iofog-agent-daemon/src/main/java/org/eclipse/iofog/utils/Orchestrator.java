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
package org.eclipse.iofog.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
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
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.field_agent.enums.RequestType;
import org.eclipse.iofog.network.IOFogNetworkInterface;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.iofog.utils.logging.LoggingService.logError;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;
import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;

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
    	logInfo(MODULE_NAME, "Inside ping");
        try {
            JsonObject result = getJSON(controllerUrl + "status");
            logInfo(MODULE_NAME, "Finished pinging");
            return !result.isNull("status");
        } catch (Exception exp) {
            logError(MODULE_NAME, "Error pinging", new AgentSystemException(exp.getMessage(), exp));
            throw exp;
        }
    }

    /**
     * does provisioning
     *
     * @param key - provisioning key
     * @return result in Json format
     * @throws AgentSystemException
     */
    public JsonObject provision(String key) throws AgentSystemException {
    	logInfo(MODULE_NAME, "Inside provision");
        try {
			JsonObject result;
			JsonObject json = Json.createObjectBuilder()
			        .add("key", key)
			        .add("type", Configuration.getFogType().getCode())
			        .build();

			result = request("provision", RequestType.POST, null, json);
			logInfo(MODULE_NAME, "Finished provision");
			return result;
		} catch (Exception e) {
			logError(MODULE_NAME, "Error while provision", new AgentSystemException(e.getMessage(), e));
            throw new AgentSystemException(e.getMessage(), e);
		}
    }

    private RequestConfig getRequestConfig() throws Exception {
    	logInfo(MODULE_NAME, "get request config");
        return RequestConfig.copy(RequestConfig.DEFAULT)
                .setLocalAddress(IOFogNetworkInterfaceManager.getInstance().getInetAddress())
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .build();
    }

    /**
     * initialize {@link TrustManager}
     *
     * @throws Exception
     */
    private void initialize(boolean secure) throws AgentSystemException {
    	logInfo(MODULE_NAME, "Start initialize TrustManager");
        if (secure) {
            TrustManager[] trustManager = new TrustManager[]{new X509TrustManagerImpl(controllerCert)};
            SSLContext sslContext;
			try {
				sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, trustManager, new SecureRandom());
				SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
	            client = HttpClients.custom().setSSLSocketFactory(sslsf).build();
			} catch (NoSuchAlgorithmException e) {
				throw new AgentSystemException(e.getMessage(), e );
			} catch (KeyManagementException e) {
				throw new AgentSystemException(e.getMessage(), e );
			}

        } else {
            client = HttpClients.createDefault();
        }
        logInfo(MODULE_NAME, "Finished initialize TrustManager");
    }

    /**
     * converts {@link InputStream} to {@link Certificate}
     *
     * @param is - {@link InputStream}
     * @return {@link Certificate}
     */
    private Certificate getCert(InputStream is) {
    	logInfo(MODULE_NAME, "Start get Certificate");
        Certificate result = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            result = certificateFactory.generateCertificate(is);
        } catch (CertificateException exp) {
            logError(MODULE_NAME, "unable to get certificate",
        			new AgentUserException("unable to get certificate", exp));
        }
        logInfo(MODULE_NAME, "Finished get Certificate");
        return result;
    }

    /**
     * gets Json result of a IOFog Controller endpoint
     *
     * @param surl - endpoind to be called
     * @return result in Json format
     * @throws AgentSystemException
     */
    private JsonObject getJSON(String surl) throws AgentUserException  {
    	logInfo(MODULE_NAME, "Start getJSON for result of a IOFog Controller endpoint");
        // disable certificates for dev mode
        boolean secure = true;
        if (!surl.toLowerCase().startsWith("https")) {
            if (!Configuration.isDeveloperMode()) {
            	logError(MODULE_NAME, "unable to connect over non-secure connection",
            			new AgentUserException("unable to connect over non-secure connection", null));
                throw new AgentUserException("unable to connect over non-secure connection", null );
            } else
                secure = false;
        }

        JsonObject result = null;

        try  {
        	initialize(secure);
            RequestConfig config = getRequestConfig();
            HttpGet get = new HttpGet(surl);
            get.setConfig(config);
        	CloseableHttpResponse response = client.execute(get);

            if (response !=null && response.getStatusLine().getStatusCode() != 200) {
                if (response.getStatusLine().getStatusCode() == 404) {
                	logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
                			new AgentUserException("unable to connect to IOFog Controller endpoint", null));
                    throw new AgentUserException("unable to connect to IOFog Controller endpoint" ,
                    		new UnknownHostException());
                } else {
                	logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
                			new AgentUserException("unable to connect to IOFog Controller endpoint", null));
                    throw new AgentUserException("unable to connect to IOFog Controller endpoint" , null);
                }
            }

            Reader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            JsonReader jsonReader = Json.createReader(in);
            result = jsonReader.readObject();


        } catch (UnsupportedEncodingException e) {
        	logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
        			new AgentUserException("unable to connect to IOFog Controller endpoint", e));
        	throw new AgentUserException("unable to connect to IOFog Controller endpoint", e );

    	} catch (UnsupportedOperationException e) {
    		logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
        			new AgentUserException("unable to connect to IOFog Controller endpoint", e));
    		throw new AgentUserException("unable to connect to IOFog Controller endpoint", e );

    	} catch (ClientProtocolException e) {
    		logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
        			new AgentUserException("unable to connect to IOFog Controller endpoint", e));
    		throw new AgentUserException("unable to connect to IOFog Controller endpoint", e );

    	} catch (IOException e) {
            try {
                IOFogNetworkInterfaceManager.getInstance().updateIOFogNetworkInterface();
            } catch (SocketException ex) {
                LoggingService.logWarning(MODULE_NAME, "Unable to update network interface : " + ex.getMessage());
            }
            logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
        			new AgentUserException("unable to connect to IOFog Controller endpoint", e));
    		throw new AgentUserException("unable to connect to IOFog Controller endpoint", e );

    	}catch (Exception e) {
    		logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
        			new AgentUserException("unable to connect to IOFog Controller endpoint", e));
    		throw new AgentUserException("unable to connect to IOFog Controller endpoint", e );
    	}
        logInfo(MODULE_NAME, "Finished getJSON for result of a IOFog Controller endpoint");
        return result;
    }

    public JsonObject request(String command, RequestType requestType, Map<String, Object> queryParams, JsonObject json) throws Exception {
    	logInfo(MODULE_NAME, "get request JsonObject");
    	if (json == null) {
            json = Json.createObjectBuilder().build();
        }
    	logInfo(MODULE_NAME, "finished request JsonObject");
        return getJsonObject(queryParams, requestType, new StringEntity(json.toString(), ContentType.APPLICATION_JSON), createUri(command));
    }

    private StringBuilder createUri(String command) {
    	logInfo(MODULE_NAME, "Create Uri");
        StringBuilder uri = new StringBuilder(controllerUrl);
        uri.append("agent/")
                .append(command);
        return uri;
    }


    private JsonObject getJsonObject(Map<String, Object> queryParams, RequestType requestType, HttpEntity httpEntity, StringBuilder uri) throws Exception {
        // disable certificates for dev mode
    	logInfo(MODULE_NAME, "Start get JsonObject");
        boolean secure = true;
        if (!controllerUrl.toLowerCase().startsWith("https")) {
            if (!Configuration.isDeveloperMode())
                throw new AgentUserException("unable to connect over non-secure connection", null);
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

        UUID requestId = UUID.randomUUID();
        req.addHeader("Request-Id", requestId.toString());
        logInfo("Orchestrator", String.format("(%s) %s %s", requestId, requestType.name(), uri.toString()));

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
                    logWarning(MODULE_NAME, "Invalid authentication ioFog token, switching controller status to Not provisioned");
                    FieldAgent.getInstance().deProvision(true);
                    throw new AuthenticationException(errorMessage);
                case 403:
                    throw new ForbiddenException(errorMessage);
                case 404:
                    throw new NotFoundException(errorMessage);
                case 500:
                    throw new InternalServerErrorException(errorMessage);
            }

        } catch (UnsupportedEncodingException exp) {
            logError(MODULE_NAME, "Error while executing the request", new AgentUserException(exp.getMessage(), exp));
            throw new AgentUserException(exp.getMessage(), exp);
        }
        logInfo(MODULE_NAME, "Finish get JsonObject");
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
    	logInfo(MODULE_NAME, "Start send file to Controller");
        InputStream inputStream = new FileInputStream(file);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addBinaryBody("upstream", inputStream, ContentType.create("application/zip"), file.getName());

        HttpEntity entity = builder.build();

        getJsonObject(null, RequestType.PUT, entity, createUri(command));
        logInfo(MODULE_NAME, "Finished send file to Controller");
    }

    /**
     * updates local variables when changes applied
     */
    public void update() {
    	logInfo(MODULE_NAME, "Start updates local variables when changes applied");
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
        } catch (AgentSystemException exp) {
        	logError(MODULE_NAME,"Error while updating local variables when changes applied",
            		new AgentUserException("Error while updating local variables when changes applied", exp));
        } catch (Exception exp) {
            logError(MODULE_NAME,"Error while updating local variables when changes applied",
            		new AgentUserException("Error while updating local variables when changes applied", exp));
        }
        logInfo(MODULE_NAME, "Finished updates local variables when changes applied");
    }
}
