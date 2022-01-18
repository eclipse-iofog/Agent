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
package org.eclipse.iofog.local_api.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.json.Json;
import javax.json.JsonObject;

import org.eclipse.iofog.message_bus.Message;

public class RestPublishTest {
	private int count;
	private int delay;
	private String publisher;
	
	public RestPublishTest(String publisher, int count, int delay) {
		this.count = count;
		this.delay = delay;
		this.publisher = publisher;
	}
	
	private final Runnable publish = () -> {
		byte[] content = Base64.getEncoder().encode("HELLLLOOOOOO!".getBytes());
		Message msg = new Message();
		msg.setPublisher(publisher);
		msg.setInfoType("test");
		msg.setInfoFormat("utf-8");
		msg.setContentData(content);
		byte[] bytes = msg.toString().getBytes(StandardCharsets.US_ASCII);
		
		try {
			for (int i = 0; i < count; i++) {
				HttpURLConnection httpRequest = (HttpURLConnection) new URL("http://127.0.0.1:54321/v2/messages/new").openConnection();
				httpRequest.setRequestMethod("POST");
				httpRequest.setRequestProperty("Content-Type", "application/json");
				httpRequest.setRequestProperty("Content-Length", String.valueOf(bytes.length));
				httpRequest.setDoOutput(true);
				httpRequest.getOutputStream().write(bytes);
				BufferedReader in = new BufferedReader(new InputStreamReader(httpRequest.getInputStream(), "UTF-8"));
				JsonObject result = Json.createReader(in).readObject();
				try {
					Thread.sleep(delay);
				} catch (Exception e) {
					break;
				}
			}
		} catch (Exception e) {}
	};
	
	public void start() {
		Thread t = new Thread(publish, "PUBLSHER");
		t.start();
	}

}
