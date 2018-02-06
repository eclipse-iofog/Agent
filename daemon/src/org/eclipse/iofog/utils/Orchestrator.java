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
package org.eclipse.iofog.utils;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.NoContentException;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.utils.trustmanager.X509TrustManagerImpl;

/**
 * provides methods for IOFog controller
 * 
 * @author saeid
 *
 */
public class Orchestrator {
	private final int CONNECTION_TIMEOUT = 5000;
	private String controllerUrl;
	private String instanceId;
	private String accessToken;
	private Certificate controllerCert;
	private static String eth;
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
			return result.getString("status").equals("ok");
		} catch (Exception e) {
			throw e;
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
		try {
			result = getJSON(controllerUrl + "instance/provision/key/" + key + "/fabrictype/" + Constants.FOG_TYPE);
		} catch (Exception e) {
			throw e;
		} 
		return result;
	}

	/**
	 * returns IPv4 host address of IOFog network interface
	 *
	 * @return {@link Inet4Address}
	 * @throws Exception
	 */
	public static String getCurrentIpAddress() {
		Optional<InetAddress> inetAddress = getLocalIp();
		return inetAddress.map(InetAddress::getHostAddress).orElse("");
	}

	private static Optional<InetAddress> getLocalIp(){
		Optional<InetAddress> inetAddress = Optional.empty();
		try {
			inetAddress = Optional.of(Orchestrator.getInetAddress());
		} catch (SocketException exp) {
			LoggingService.logWarning(MODULE_NAME, "Unable to find the IP address of the machine running ioFog: " + exp.getMessage());
		}
		return inetAddress;
	}

	/**
	 * returns IPv4 address of IOFog network interface
	 * 
	 * @return {@link Inet4Address}
	 * @throws Exception
	 */
	private static InetAddress getInetAddress() throws SocketException {
		Enumeration<InetAddress> ipAddresses = NetworkInterface.getByName(eth)
				.getInetAddresses();
		while (ipAddresses.hasMoreElements()) {
			InetAddress address = ipAddresses.nextElement();
			if (address instanceof Inet4Address) {
				return address;
			}
		}
		throw new ConnectException(String.format("unable to get ip address \"%s\"", eth));
	}
	
	private RequestConfig getRequestConfig() throws Exception {
		return RequestConfig.copy(RequestConfig.DEFAULT)
				.setLocalAddress(getInetAddress())
				.setConnectTimeout(CONNECTION_TIMEOUT)
				.build();
	}
	
	/**
	 * initialize {@link TrustManager}
	 * 
	 * @throws Exception
	 */
	private void initialize() throws Exception {
		TrustManager[] trustManager = new TrustManager[] {new X509TrustManagerImpl(controllerCert)};
        SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, trustManager, new SecureRandom());
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
	    client = HttpClients.custom().setSSLSocketFactory(sslsf).build();
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
			LoggingService.logWarning(MODULE_NAME, exp.getMessage());
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
		if (!surl.toLowerCase().startsWith("https"))
			throw new UnknownHostException("unable to connect over non-secure connection");
		initialize();
		RequestConfig config = getRequestConfig();
		HttpPost post = new HttpPost(surl);
		post.setConfig(config);

		JsonObject result;
		
		try (CloseableHttpResponse response = client.execute(post)) {

			if (response.getStatusLine().getStatusCode() != 200) {
				if (response.getStatusLine().getStatusCode() == 404)
					throw new UnknownHostException();
				else
					throw new Exception();
			}

			try(Reader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				JsonReader jsonReader = Json.createReader(in)){
				result = jsonReader.readObject();
			}

		}
		return result;
	}

	/**
	 * calls IOFog Controller endpoind and returns Json result
	 * 
	 * @param command - endpoint to be called
	 * @param queryParams - query string parameters
	 * @param postParams - post parameters
	 * @return result in Json format
	 * @throws Exception
	 */
	public JsonObject doCommand(String command, Map<String, Object> queryParams, Map<String, Object> postParams) throws Exception {
		if (!controllerUrl.toLowerCase().startsWith("https"))
			throw new Exception("unable to connect over non-secure connection");
		JsonObject result;
		
		StringBuilder uri = new StringBuilder(controllerUrl);
		
		uri.append("instance/")
			.append(command)
			.append("/id/").append(instanceId)
			.append("/token/").append(accessToken);
		
		if (queryParams != null)
			queryParams.forEach((key, value) -> uri.append("/").append(key)
					.append("/").append(value));

		List<NameValuePair> postData = new ArrayList<NameValuePair>();		
		if (postParams != null)
			postParams.forEach((key, value1) -> {
				String value = value1.toString();
				if (value == null)
					value = "";
				postData.add(new BasicNameValuePair(key, value));
			});


		initialize();
		RequestConfig config = getRequestConfig();
		HttpPost post = new HttpPost(uri.toString());
		post.setConfig(config);
		post.setEntity(new UrlEncodedFormEntity(postData));

		try (CloseableHttpResponse response = client.execute(post);
			 BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			 JsonReader jsonReader = Json.createReader(in)) {

			if (response.getStatusLine().getStatusCode() == 403) {
				throw new ForbiddenException();
			} else if (response.getStatusLine().getStatusCode() == 204) {
				throw new NoContentException("");
			}


			result = jsonReader.readObject();
		} catch (Exception e) {
			throw e;
		}
		
		return result;
	}

	/**
	 * updates local variables when changes applied
	 * 
	 */
	public void update() {
		instanceId = Configuration.getInstanceId();
		accessToken = Configuration.getAccessToken();
		controllerUrl = Configuration.getControllerUrl();
		try (FileInputStream fileInputStream = new FileInputStream(Configuration.getControllerCert())) {
			controllerCert = getCert(fileInputStream);
		} catch (IOException e) {
			controllerCert = null;
		} 
		eth = Configuration.getNetworkInterface();
		try {
			initialize();
		} catch (Exception e) {}
	}
}
