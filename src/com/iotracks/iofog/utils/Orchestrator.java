package com.iotracks.iofog.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
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

import com.iotracks.iofog.utils.configuration.Configuration;

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
		JsonObject result = null;
		try {
			result = getJSON(controllerUrl + "instance/provision/key/" + key + "/fogtype/" + Constants.getFogType());
		} catch (Exception e) {
			throw e;
		} 
		return result;
	}
	
	/**
	 * returns IPv4 address of IOFog network interface
	 * 
	 * @return {@link Inet4Address}
	 * @throws Exception
	 */
	public static InetAddress getInetAddress() throws Exception {
		InetAddress address = null;
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		    while (networkInterfaces.hasMoreElements()) {
		        NetworkInterface networkInterface = networkInterfaces.nextElement();
		        if (networkInterface.getName().equals(eth)) {
		        	Enumeration<InetAddress> ipAddresses = networkInterface.getInetAddresses();
		        	while (ipAddresses.hasMoreElements()) {
		        		address = ipAddresses.nextElement();
		        		if (address instanceof Inet4Address) {
		        			return address;
		        		}
		        	}
		        }
		    }
		} catch (Exception e) {
		}
		throw new ConnectException(String.format("unable to get ip address \"%s\"", eth));
	}
	
	private RequestConfig getRequestConfig() throws Exception {
		return RequestConfig.copy(RequestConfig.DEFAULT)
				.setLocalAddress(getInetAddress())
//				.setConnectionRequestTimeout(CONNECTION_TIMEOUT)
//				.setSocketTimeout(CONNECTION_TIMEOUT)
				.setConnectTimeout(CONNECTION_TIMEOUT)
				.build();
	}
	
	/**
	 * initialize {@link TrustManager}
	 * 
	 * @throws Exception
	 */
	private void initialize() throws Exception {
		TrustManager[] trustManager = new TrustManager[] {
				new X509TrustManager() {
					private X509Certificate[] certs;
					
					@Override
					public X509Certificate[] getAcceptedIssuers() {
						return certs;
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
				}
		}; 
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
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			Certificate result = certificateFactory.generateCertificate(is);
			return result;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * gets Json result of a IOFog Controller endpoint
	 * 
	 * @param surl - endpoind to be called
	 * @return result in Json format
	 * @throws Exception
	 */
	public JsonObject getJSON(String surl) throws Exception {
		if (!surl.toLowerCase().startsWith("https"))
			throw new UnknownHostException("unable to connect over non-secure connection");
		initialize();
		RequestConfig config = getRequestConfig();
		HttpPost post = new HttpPost(surl);
		post.setConfig(config);
		
		CloseableHttpResponse response = client.execute(post);
		
		if (response.getStatusLine().getStatusCode() != 200) {
			if (response.getStatusLine().getStatusCode() == 404)
				throw new UnknownHostException();
			else
				throw new Exception();
		}
		
        Reader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

        JsonObject result = Json.createReader(in).readObject();
        
        response.close();
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
		JsonObject result = null;
		
		StringBuilder uri = new StringBuilder(controllerUrl);
		
		uri.append("instance/")
			.append(command)
			.append("/id/").append(instanceId)
			.append("/token/").append(accessToken);
		
		if (queryParams != null)
			queryParams.entrySet().forEach(entry -> {
				uri.append("/").append(entry.getKey())
					.append("/").append(entry.getValue());
			});

		List<NameValuePair> postData = new ArrayList<NameValuePair>();		
		if (postParams != null)
			postParams.entrySet().forEach(entry -> {
				String value = entry.getValue().toString();
				if (value == null)
					value = "";
				postData.add(new BasicNameValuePair(entry.getKey(), value));
			});
		
	
		try {
			initialize();
			RequestConfig config = getRequestConfig();
			HttpPost post = new HttpPost(uri.toString());
			post.setConfig(config);
			post.setEntity(new UrlEncodedFormEntity(postData));

			CloseableHttpResponse response = client.execute(post);
			
			if (response.getStatusLine().getStatusCode() == 403){
				throw new ForbiddenException();
			} else if (response.getStatusLine().getStatusCode() == 204){
				throw new NoContentException("");
			}
			
			BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			result = Json.createReader(in).readObject();
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
		try {
			controllerCert = getCert(new FileInputStream(Configuration.getControllerCert()));
		} catch (FileNotFoundException e) {
			controllerCert = null;
		} 
		eth = Configuration.getNetworkInterface();
		try {
			initialize();
		} catch (Exception e) {}
	}
}
