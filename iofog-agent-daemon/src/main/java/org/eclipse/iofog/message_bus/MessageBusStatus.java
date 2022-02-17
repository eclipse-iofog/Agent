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
package org.eclipse.iofog.message_bus;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

/**
 * represents Message Bus status
 * 
 * @author saeid
 *
 */
public class MessageBusStatus {
	private long processedMessages;
	private final Map<String, Long> publishedMessagesPerMicroservice;
	private float averageSpeed;
	
	public MessageBusStatus() {
		publishedMessagesPerMicroservice = new HashMap<>();
		processedMessages = 0;
		averageSpeed = 0;
	}
	
	public long getProcessedMessages() {
		return processedMessages;
	}

	public Long getPublishedMessagesPerMicroservice(String microservice) {
		return publishedMessagesPerMicroservice.get(microservice);
	}

	public Map<String, Long> getPublishedMessagesPerMicroservice() {
		return publishedMessagesPerMicroservice;
	}

	public MessageBusStatus increasePublishedMessagesPerMicroservice(String microservice) {
		this.processedMessages++;

		Long n = this.publishedMessagesPerMicroservice.get(microservice);
		if (n == null)
			n = 0L;
		this.publishedMessagesPerMicroservice.put(microservice, n + 1);
		return this;
	}

	public float getAverageSpeed() {
		return averageSpeed;
	}

	public MessageBusStatus setAverageSpeed(float averageSpeed) {
		this.averageSpeed = averageSpeed;
		return this;
	}
	
	public void removePublishedMessagesPerMicroservice(String microservice) {
		if (publishedMessagesPerMicroservice.containsKey(microservice))
			publishedMessagesPerMicroservice.remove(microservice);
	}
	
	public String getJsonPublishedMessagesPerMicroservice() {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		publishedMessagesPerMicroservice.forEach((key, value) -> {
			JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
					.add("id", key)
					.add("messagecount", value);
			arrayBuilder.add(objectBuilder);

		});
		return arrayBuilder.build().toString();
	}

}
