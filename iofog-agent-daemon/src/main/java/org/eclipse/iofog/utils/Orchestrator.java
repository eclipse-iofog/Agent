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
package org.eclipse.iofog.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.Header;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.field_agent.enums.RequestType;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.utils.trustmanager.TrustManagers;
import org.eclipse.iofog.utils.JwtManager;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonParsingException;
import javax.naming.AuthenticationException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLException;
import java.util.Base64;

import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.eclipse.iofog.utils.logging.LoggingService.*;

/**
 * provides methods for IOFog controller
 *
 * @author saeid
 */
public class Orchestrator {
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    // Socket timeout (read timeout) set to 5 minutes - less than JWT expiry of 10 minutes
    // This prevents requests from hanging long enough for JWT tokens to expire
    private static final int SOCKET_TIMEOUT = 5 * 60 * 1000; // 5 minutes
    // Request-level timeout covers DNS resolution, connection, and read phases
    private static final int REQUEST_TIMEOUT_SECONDS = 5 * 60; // 5 minutes (same as socket timeout)
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    private String controllerUrl;
    private String iofogUuid;
    // private String iofogAccessToken;
    private String iofogPrivateKey;
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
    	logDebug(MODULE_NAME, "Inside ping");
        try {
            JsonObject result = getJSON(controllerUrl + "status");
            logDebug(MODULE_NAME, "Finished pinging");
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
    	logDebug(MODULE_NAME, "Inside provision");
        try {
			JsonObject result;
			JsonObject json = Json.createObjectBuilder()
			        .add("key", key)
			        .add("type", Configuration.getArch().getCode())
			        .build();

			result = request("provision", RequestType.POST, null, json);
			logDebug(MODULE_NAME, "Finished provision");
			return result;
		} catch (Exception e) {
			logError(MODULE_NAME, "Error while provision", new AgentSystemException(e.getMessage(), e));
            throw new AgentSystemException(e.getMessage(), e);
		}
    }

    private RequestConfig getRequestConfig() throws Exception {
    	logDebug(MODULE_NAME, "get request config");
        return RequestConfig.copy(RequestConfig.DEFAULT)
                .setLocalAddress(IOFogNetworkInterfaceManager.getInstance().getInetAddress())
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)  // Read timeout - prevents requests from hanging indefinitely
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT)  // Timeout for getting connection from pool
                .build();
    }

    /**
     * initialize {@link TrustManager}
     *
     * @throws Exception
     */
    private void initialize(boolean secure) throws AgentSystemException {
    	logDebug(MODULE_NAME, "Start initialize TrustManager");
        if (secure) {
            SSLContext sslContext;
			try {
				sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, TrustManagers.createTrustManager(controllerCert), new SecureRandom());
				SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
	            client = HttpClients.custom().setSSLSocketFactory(sslsf).build();
			} catch (Exception e) {
				throw new AgentSystemException(e.getMessage(), e );		
			}
            
        } else {
            client = HttpClients.createDefault();
        }
        logDebug(MODULE_NAME, "Finished initialize TrustManager");
    }

    /**
     * converts {@link InputStream} to {@link Certificate}
     *
     * @param is - {@link InputStream}
     * @return {@link Certificate}
     */
    private Certificate getCert(InputStream is) {
    	logDebug(MODULE_NAME, "Start get Certificate");
        Certificate result = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            result = certificateFactory.generateCertificate(is);
        } catch (CertificateException exp) {
            logError(MODULE_NAME, "unable to get certificate",
        			new AgentUserException(exp.getMessage(), exp));
        }
        logDebug(MODULE_NAME, "Finished get Certificate");
        return result;
    }

    /**
     * gets Json result of a IOFog Controller endpoint with request-level timeout protection
     * This wraps the actual request in a Future with timeout to prevent hanging on DNS resolution,
     * connection establishment, or response reading.
     *
     * @param surl - endpoint to be called
     * @return result in Json format
     * @throws AgentUserException 
     */
    private JsonObject getJSON(String surl) throws AgentUserException {
        return getJSONWithTimeout(surl);
    }

    /**
     * Internal method that wraps getJSONInternal in a Future with timeout protection
     *
     * @param surl - endpoint to be called
     * @return result in Json format
     * @throws AgentUserException
     */
    private JsonObject getJSONWithTimeout(String surl) throws AgentUserException {
        logDebug(MODULE_NAME, "Start getJSONWithTimeout for result of a IOFog Controller endpoint");
        
        Future<JsonObject> future = executorService.submit(() -> {
            return getJSONInternal(surl);
        });
        
        try {
            // Wait for the request with a total timeout that includes DNS resolution, connection, and read
            return future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            logError(MODULE_NAME, "Request timeout after " + REQUEST_TIMEOUT_SECONDS + " seconds", 
                    new AgentSystemException("Request timeout: " + surl, e));
            throw new AgentUserException("Request timeout after " + REQUEST_TIMEOUT_SECONDS + " seconds: " + surl, e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AgentUserException) {
                throw (AgentUserException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new AgentUserException("Error executing request: " + surl, cause);
            }
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new AgentUserException("Request interrupted: " + surl, e);
        }
    }

    /**
     * Internal method that performs the actual HTTP request
     *
     * @param surl - endpoint to be called
     * @return result in Json format
     * @throws AgentUserException 
     */
    private JsonObject getJSONInternal(String surl) throws AgentUserException  {
    	logDebug(MODULE_NAME, "Start getJSONInternal for result of a IOFog Controller endpoint");
        // disable certificates for secure mode
        boolean secure = true;
        if (!surl.toLowerCase().startsWith("https")) {
            if (Configuration.isSecureMode()) {
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
        			new AgentUserException(e.getMessage(), e));
        	throw new AgentUserException(e.getMessage(), e );

    	} catch (UnsupportedOperationException e) {
    		logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
        			new AgentUserException(e.getMessage(), e));
    		throw new AgentUserException(e.getMessage(), e );

    	} catch (ClientProtocolException e) {
    		logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
        			new AgentUserException(e.getMessage(), e));
    		throw new AgentUserException(e.getMessage(), e );
    		
    	} catch (CertificateException e) {
            // Only renew for actual certificate validation failures
            logWarning(MODULE_NAME, "Certificate validation failed, attempting to renew certificate");
            try {
                // First, initialize with insecure SSL context to get the new certificate
                initialize(false);
                
                // Get new certificate from controller
                String base64Cert = getControllerCert();
                
                // Save the new certificate
                try (FileOutputStream fos = new FileOutputStream(Configuration.getControllerCert())) {
                    byte[] certBytes = Base64.getDecoder().decode(base64Cert);
                    fos.write(certBytes);
                }
                
                // Update SSL context with the new certificate
                update();
                
                // Ensure secure mode is enabled after successful renewal
                Configuration.setSecureMode(true);
                
                // Retry the original request
                logInfo(MODULE_NAME, "Certificate renewed successfully, retrying request");
                return getJSON(surl);
            } catch (Exception ex) {
                logError(MODULE_NAME, "Failed to update certificate", ex);
                throw new AgentUserException("Failed to update certificate: " + ex.getMessage(), ex);
            }
    		
    	} catch (SSLHandshakeException e) {
            // Handle SSL handshake failures separately (not certificate issues)
            logError(MODULE_NAME, "SSL handshake failed", e);
            throw new AgentUserException("SSL handshake failed: " + e.getMessage(), e);
    		
    	} catch (IOException e) {
            try {
                IOFogNetworkInterfaceManager.getInstance().updateIOFogNetworkInterface();
            } catch (SocketException | MalformedURLException ex) {
                LoggingService.logWarning(MODULE_NAME, "Unable to update network interface : " + ex.getMessage());
            }
            logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
        			new AgentUserException(e.getMessage(), e));
    		throw new AgentUserException(e.getMessage(), e );
    		
    	}catch (Exception e) {
    		logError(MODULE_NAME, "unable to connect to IOFog Controller endpoint",
        			new AgentUserException(e.getMessage(), e));
    		throw new AgentUserException(e.getMessage(), e );
    	}
        logDebug(MODULE_NAME, "Finished getJSON for result of a IOFog Controller endpoint");
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
        return getJsonObjectWithTimeout(queryParams, requestType, httpEntity, uri);
    }

    /**
     * Internal method that wraps getJsonObjectInternal in a Future with timeout protection
     *
     * @param queryParams - query parameters
     * @param requestType - HTTP request type
     * @param httpEntity - HTTP entity (body)
     * @param uri - request URI
     * @return result in Json format
     * @throws Exception
     */
    private JsonObject getJsonObjectWithTimeout(Map<String, Object> queryParams, RequestType requestType, HttpEntity httpEntity, StringBuilder uri) throws Exception {
        logDebug(MODULE_NAME, "Start getJsonObjectWithTimeout");
        
        // Create a final copy of the URI for use in the Future
        final StringBuilder finalUri = new StringBuilder(uri.toString());
        final Map<String, Object> finalQueryParams = queryParams;
        final RequestType finalRequestType = requestType;
        final HttpEntity finalHttpEntity = httpEntity;
        
        Future<JsonObject> future = executorService.submit(() -> {
            return getJsonObjectInternal(finalQueryParams, finalRequestType, finalHttpEntity, finalUri);
        });
        
        try {
            // Wait for the request with a total timeout that includes DNS resolution, connection, and read
            return future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            logError(MODULE_NAME, "Request timeout after " + REQUEST_TIMEOUT_SECONDS + " seconds", 
                    new AgentSystemException("Request timeout: " + finalUri.toString(), e));
            throw new AgentSystemException("Request timeout after " + REQUEST_TIMEOUT_SECONDS + " seconds: " + finalUri.toString(), e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new AgentSystemException("Error executing request: " + finalUri.toString(), cause);
            }
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new AgentSystemException("Request interrupted: " + finalUri.toString(), e);
        }
    }

    /**
     * Internal method that performs the actual HTTP request
     *
     * @param queryParams - query parameters
     * @param requestType - HTTP request type
     * @param httpEntity - HTTP entity (body)
     * @param uri - request URI
     * @return result in Json format
     * @throws Exception
     */
    private JsonObject getJsonObjectInternal(Map<String, Object> queryParams, RequestType requestType, HttpEntity httpEntity, StringBuilder uri) throws Exception {
        // disable certificates for secure mode
    	logDebug(MODULE_NAME, "Start get JsonObjectInternal");
        boolean secure = true;
        if (!controllerUrl.toLowerCase().startsWith("https")) {
            if (Configuration.isSecureMode())
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

        // Generate and add JWT token only for non-provisioning requests
        // Specifically exclude /agent/provision but include /agent/deprovision
        if (!uri.toString().endsWith("/agent/provision") && !uri.toString().endsWith("/api/v3/status")) {
            String jwtToken = JwtManager.generateJwt();
            if (jwtToken == null) {
                logError(MODULE_NAME, "Failed to generate JWT token", new AgentSystemException("Failed to generate JWT token"));
                throw new AuthenticationException("Failed to generate JWT token");
            }
            req.addHeader(new BasicHeader("Authorization", "Bearer " + jwtToken));
        }

        UUID requestId = UUID.randomUUID();
        req.addHeader("Request-Id", requestId.toString());
        logDebug("Orchestrator", String.format("(%s) %s %s", requestId, requestType.name(), uri.toString()));

        try (CloseableHttpResponse response = client.execute(req)) {
            String errorMessage = "";
            HttpEntity responseBody = response.getEntity();
            if (responseBody != null) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"))) {
                    JsonReader jsonReader = Json.createReader(in);
                    result = jsonReader.readObject();
                    errorMessage = result.getString("message", "");
                } catch (JsonException e) {
                    logInfo(MODULE_NAME, "get config response contains non JSON payload, content-type: " + responseBody.getContentType());
                }
            }

            int statusCode = response.getStatusLine().getStatusCode();
            switch (statusCode) {
                case 204:
                    return Json.createObjectBuilder().build();
                case 400:
                    throw new BadRequestException(errorMessage);
                case 401:
                    // TODO: Add retry logic with fresh token
                    FieldAgent.getInstance().deProvision(true);
                    logWarning(MODULE_NAME, "Invalid JWT token, switching controller status to Not provisioned");
                    throw new AuthenticationException(errorMessage);
                case 403:
                    throw new ForbiddenException(errorMessage);
                case 404:
                    throw new NotFoundException(errorMessage);
                case 500:
                    throw new InternalServerErrorException(errorMessage);
                default:
                    if (statusCode >= 400 && statusCode < 500) {
                        throw new ClientErrorException(response.getStatusLine().getReasonPhrase(), statusCode);
                    } else if (statusCode >= 500 && statusCode < 600) {
                        throw new ServerErrorException(response.getStatusLine().getReasonPhrase(), statusCode);
                    }
            }

        } catch (UnsupportedEncodingException exp) {
            logError(MODULE_NAME, "Error while executing the request", new AgentUserException(exp.getMessage(), exp));
            throw new AgentUserException(exp.getMessage(), exp);
        }
        logDebug(MODULE_NAME, "Finish get JsonObject");
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
    	logDebug(MODULE_NAME, "Start send file to Controller");
        InputStream inputStream = new FileInputStream(file);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addBinaryBody("upstream", inputStream, ContentType.create("application/zip"), file.getName());

        HttpEntity entity = builder.build();

        getJsonObject(null, RequestType.PUT, entity, createUri(command));
        logDebug(MODULE_NAME, "Finished send file to Controller");
    }

    /**
     * updates local variables when changes applied
     */
    public void update() {
    	logDebug(MODULE_NAME, "Start updates local variables when changes applied");
        iofogUuid = Configuration.getIofogUuid();
        // iofogAccessToken = Configuration.getAccessToken();
        iofogPrivateKey = Configuration.getPrivateKey();
        controllerUrl = Configuration.getControllerUrl();
        // disable certificates for secure mode
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
            		new AgentUserException(exp.getMessage(), exp));
        } catch (Exception exp) {
            logError(MODULE_NAME,"Error while updating local variables when changes applied", 
            		new AgentUserException(exp.getMessage(), exp));
        }
        logDebug(MODULE_NAME, "Finished updates local variables when changes applied");
    }

    /**
     * Gets the controller's certificate using an insecure connection
     * This is used when the current certificate is invalid and we need to get a new one
     * 
     * @return base64 encoded certificate string
     * @throws AgentSystemException if the request fails
     */
    public String getControllerCert() throws Exception {
        String response = null;
        try {
            StringBuilder uri = createUri("cert");
            HttpGet request = new HttpGet(uri.toString());
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + JwtManager.generateJwt());

            CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(TrustManagers.getInsecureSocketFactory())
                .disableConnectionState()
                .setConnectionReuseStrategy((resp, context) -> false)
                .disableCookieManagement()
                .build();

            try (CloseableHttpResponse httpResponse = httpClient.execute(request)) {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                switch (statusCode) {
                    case 200:
                        response = EntityUtils.toString(httpResponse.getEntity());
                        if (response == null || response.isEmpty()) {
                            throw new AgentSystemException("Empty response from controller");
                        }
                        return response;
                    case 401:
                        FieldAgent.getInstance().deProvision(true);
                        logWarning(MODULE_NAME, "Invalid JWT token, switching controller status to Not provisioned");
                        throw new AuthenticationException("Unauthorized access to controller certificate");
                    case 404:
                        throw new AgentSystemException("Controller not found", new UnknownHostException());
                    case 400:
                        throw new BadRequestException("Invalid request for controller certificate");
                    case 403:
                        throw new ForbiddenException("Access forbidden to controller certificate");
                    case 500:
                        throw new InternalServerErrorException("Internal server error while getting controller certificate");
                    default:
                        if (statusCode >= 400 && statusCode < 500) {
                            throw new ClientErrorException(httpResponse.getStatusLine().getReasonPhrase(), statusCode);
                        } else if (statusCode >= 500 && statusCode < 600) {
                            throw new ServerErrorException(httpResponse.getStatusLine().getReasonPhrase(), statusCode);
                        }
                        throw new AgentSystemException("Unexpected status code while getting controller certificate: " + statusCode);
                }
            }
        } catch (Exception e) {
            logError(MODULE_NAME, "Error getting controller certificate", e);
            throw e;
        }
    }
}
