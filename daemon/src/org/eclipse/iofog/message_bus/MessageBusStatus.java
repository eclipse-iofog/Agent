/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
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
	private final Map<String, Long> publishedMessagesPerElement;
	private float averageSpeed;
	
	public MessageBusStatus() {
		publishedMessagesPerElement = new HashMap<>();
		processedMessages = 0;
		averageSpeed = 0;
	}
	
	public long getProcessedMessages() {
		return processedMessages;
	}

	public Long getPublishedMessagesPerElement(String element) {
		return publishedMessagesPerElement.get(element);
	}

	public Map<String, Long> getPublishedMessagesPerElement() {
		return publishedMessagesPerElement;
	}

	public MessageBusStatus increasePublishedMessagesPerElement(String element) {
		this.processedMessages++;

		Long n = this.publishedMessagesPerElement.get(element);
		if (n == null)
			n = 0L;
		this.publishedMessagesPerElement.put(element, n + 1);
		return this;
	}

	public float getAverageSpeed() {
		return averageSpeed;
	}

	public MessageBusStatus setAverageSpeed(float averageSpeed) {
		this.averageSpeed = averageSpeed;
		return this;
	}
	
	public void removePublishedMessagesPerElement(String element) {
		if (publishedMessagesPerElement.containsKey(element))
			publishedMessagesPerElement.remove(element);
	}
	
	public String getJsonPublishedMessagesPerElement() {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		publishedMessagesPerElement.forEach((key, value) -> {
			JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
					.add("id", key)
					.add("messagecount", value);
			arrayBuilder.add(objectBuilder);

		});
		return arrayBuilder.build().toString();
	}

}
