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
package org.eclipse.iofog.message_bus;

import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.element.Element;
import org.eclipse.iofog.element.ElementManager;
import org.eclipse.iofog.element.Route;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.eclipse.iofog.utils.Constants.MESSAGE_BUS;
import static org.eclipse.iofog.utils.Constants.ModulesStatus.STOPPED;

/**
 * Message Bus module
 * 
 * @author saeid
 *
 */
public class MessageBus implements IOFogModule {
	
	private final String MODULE_NAME = "Message Bus";

	private MessageBusServer messageBusServer;
	private Map<String, Route> routes;
	private Map<String, MessagePublisher> publishers;
	private Map<String, MessageReceiver> receivers;
	private MessageIdGenerator idGenerator;
	private static MessageBus instance;
	private ElementManager elementManager;
	private final Object updateLock = new Object();
	
	private long lastSpeedTime, lastSpeedMessageCount;

	private MessageBus() {}

	@Override
	public int getModuleIndex() {
		return MESSAGE_BUS;
	}

	@Override
	public String getModuleName() {
		return MODULE_NAME;
	}
	
	public static MessageBus getInstance() {
		if (instance == null) {
			synchronized (MessageBus.class) {
				if (instance == null) { 
					instance = new MessageBus();
				}
			}
		}
		return instance;
	}
	
	
	/**
	 * enables real-time {@link Message} receiving of an {@link Element} 
	 * 
	 * @param receiver - ID of {@link Element}
	 */
	public synchronized void enableRealTimeReceiving(String receiver) {
		MessageReceiver rec = receivers.get(receiver); 
		if (rec == null)
			return;
		rec.enableRealTimeReceiving();
	}

	/**
	 * disables real-time {@link Message} receiving of an {@link Element} 
	 * 
	 * @param receiver - ID of {@link Element}
	 */
	public synchronized void disableRealTimeReceiving(String receiver) {
		MessageReceiver rec = receivers.get(receiver); 
		if (rec == null)
			return;
		rec.disableRealTimeReceiving();
	}

	/**
	 * initialize list of {@link Message} publishers and receivers
	 * 
	 */
	private void init() {
		lastSpeedMessageCount = 0;
		lastSpeedTime = System.currentTimeMillis();
		
		routes = elementManager.getRoutes();
		idGenerator = new MessageIdGenerator();
		publishers = new ConcurrentHashMap<>();
		receivers = new ConcurrentHashMap<>();

		if (routes == null)
			return;
		
		routes.entrySet().stream()
			.filter(route -> route.getValue() != null)
			.filter(route -> route.getValue().getReceivers() != null)
			.forEach(entry -> {
					String publisher = entry.getKey();
					Route route = entry.getValue();
				
					try {
						messageBusServer.createProducer(publisher);
					} catch (Exception e) {
						LoggingService.logWarning(MODULE_NAME + "(" + publisher + ")",
								"unable to start publisher module --> " + e.getMessage());
					}
					publishers.put(publisher, new MessagePublisher(publisher, route, messageBusServer.getProducer(publisher)));

					receivers.putAll(entry.getValue().getReceivers()
							.stream()
							.filter(item -> !receivers.containsKey(item))
							.collect(Collectors.toMap(item -> item, item -> {
								try {
									messageBusServer.createCosumer(item);
								} catch (Exception e) {
									LoggingService.logWarning(MODULE_NAME + "(" + item + ")",
											"unable to start receiver module --> " + e.getMessage());
								}
								return new MessageReceiver(item, messageBusServer.getConsumer(item));
							})));
			});

	}
	
	/**
	 * calculates the average speed of {@link Message} moving through ioFog
	 * 
	 */
	private final Runnable calculateSpeed = () -> {
		while (true) {
			try {
				Thread.sleep(Constants.SPEED_CALCULATION_FREQ_MINUTES * 60 * 1000);

				logInfo("calculating message processing speed");

				long now = System.currentTimeMillis();
				long msgs = StatusReporter.getMessageBusStatus().getProcessedMessages();

				float speed = ((float)(msgs - lastSpeedMessageCount)) / ((now - lastSpeedTime) / 1000f);
				StatusReporter.setMessageBusStatus().setAverageSpeed(speed);
				lastSpeedMessageCount = msgs;
				lastSpeedTime = now;
			} catch (Exception e) {}
		}
	};
	
	/**
	 * monitors HornetQ server
	 * 
	 */
	private final Runnable checkMessageServerStatus = () -> {
		while (true) {
			try {
				Thread.sleep(5000);

				logInfo("check message bus server status");
				if (!messageBusServer.isServerActive()) {
					logWarning("server is not active. restarting...");
					stop();
					try {
						messageBusServer.startServer();
						logInfo("server restarted");
						init();
					} catch (Exception e) {
						logWarning("server restart failed --> " + e.getMessage());
					}
				}

				publishers.entrySet().forEach(entry -> {
					String publisher = entry.getKey();
					if (messageBusServer.isProducerClosed(publisher)) {
						logWarning("producer module for " + publisher + " stopped. restarting...");
						entry.getValue().close();
						Route route = routes.get(publisher);
						if (route.equals(null) || route.getReceivers() == null || route.getReceivers().size() == 0) {
							publishers.remove(publisher);
						} else {
							try {
								messageBusServer.createProducer(publisher);
								publishers.put(publisher, new MessagePublisher(publisher, route, messageBusServer.getProducer(publisher)));
								logInfo("producer module restarted");
							} catch (Exception e) {
								logWarning("unable to restart producer module for " + publisher + " --> " + e.getMessage());
							}
						}
					}
				});

				receivers.entrySet().forEach(entry -> {
					String receiver = entry.getKey();
					if (messageBusServer.isConsumerClosed(receiver)) {
						logWarning("consumer module for " + receiver + " stopped. restarting...");
						entry.getValue().close();
						try {
							messageBusServer.createCosumer(receiver);
							receivers.put(receiver, new MessageReceiver(receiver, messageBusServer.getConsumer(receiver)));
							logInfo("consumer module restarted");
						} catch (Exception e) {
							logWarning("unable to restart consumer module for " + receiver + " --> " + e.getMessage());
						}
					}
				});
			} catch (Exception e) {
			}
		}
	};
	
	/**
	 * updates routing, list of publishers and receivers
	 * Field Agent calls this method when any changes applied
	 * 
	 */
	public void update() {
		synchronized (updateLock) {
			Map<String, Route> newRoutes = elementManager.getRoutes();
			List<String> newPublishers = new ArrayList<>();
			List<String> newReceivers = new ArrayList<>();
			
			if (newRoutes != null) {
				newRoutes.entrySet()
					.stream()
					.filter(route -> route.getValue() != null)
					.filter(route -> route.getValue().getReceivers() != null)
					.forEach(entry -> {
						newPublishers.add(entry.getKey());
						newReceivers.addAll(entry.getValue().getReceivers()
								.stream().filter(item -> !newReceivers.contains(item))
								.collect(Collectors.toList()));
					});
			}
			
			publishers.entrySet().forEach(entry -> {
				if (!newPublishers.contains(entry.getKey())) {
					entry.getValue().close();
					messageBusServer.removeProducer(entry.getKey());
				} else {
					entry.getValue().updateRoute(newRoutes.get(entry.getKey()));
				}
			});
			publishers.entrySet().removeIf(entry -> !newPublishers.contains(entry.getKey()));
			publishers.putAll(
					newPublishers.stream()
					.filter(publisher -> !publishers.containsKey(publisher))
					.collect(Collectors.toMap(publisher -> publisher, 
							publisher -> new MessagePublisher(publisher, newRoutes.get(publisher), messageBusServer.getProducer(publisher)))));

			receivers.entrySet().forEach(entry -> {
				if (!newReceivers.contains(entry.getKey())) {
					entry.getValue().close();
					messageBusServer.removeConsumer(entry.getKey());
				}
			});
			receivers.entrySet().removeIf(entry -> !newReceivers.contains(entry.getKey()));
			receivers.putAll(
					newReceivers.stream()
					.filter(receiver -> !receivers.containsKey(receiver))
					.collect(Collectors.toMap(receiver -> receiver, 
							receiver -> new MessageReceiver(receiver, messageBusServer.getConsumer(receiver)))));

			routes = newRoutes;

			List<Element> latestElements = elementManager.getLatestElements();
			StatusReporter.getMessageBusStatus()
				.getPublishedMessagesPerElement().entrySet().removeIf(entry ->
					!elementManager.elementExists(latestElements, entry.getKey()));
			latestElements.forEach(e -> {
				if (!StatusReporter.getMessageBusStatus().getPublishedMessagesPerElement().entrySet().contains(e.getElementId()))
						StatusReporter.getMessageBusStatus().getPublishedMessagesPerElement().put(e.getElementId(), 0l);
			});
		}
	}
	
	/**
	 * sets  memory usage limit of HornetQ
	 * {@link Configuration} calls this method when any changes applied
	 * 
	 */
	public void instanceConfigUpdated() {
		messageBusServer.setMemoryLimit();
	}
	
	/**
	 * starts Message Bus module
	 * 
	 */
	public void start() {
		elementManager = ElementManager.getInstance();
		
		messageBusServer = new MessageBusServer();
		try {
			logInfo("STARTING MESSAGE BUS SERVER");
			messageBusServer.startServer();
			messageBusServer.initialize();
		} catch (Exception e) {
			try {
				messageBusServer.stopServer();
			} catch (Exception e1) {}
			logWarning("unable to start message bus server --> " + e.getMessage());
			StatusReporter.setSupervisorStatus().setModuleStatus(MESSAGE_BUS, STOPPED);
		}
		
		logInfo("MESSAGE BUS SERVER STARTED");
		init();

		new Thread(calculateSpeed, "MessageBus : CalculateSpeed").start();
		new Thread(checkMessageServerStatus, "MessageBus : CheckMessageBusServerStatus").start();
	}
	
	/**
	 * closes receivers and publishers and stops HornetQ server
	 * 
	 */
	public void stop() {
		for (MessageReceiver receiver : receivers.values()) 
			receiver.close();
		
		for (MessagePublisher publisher : publishers.values())
			publisher.close();
		try {
			messageBusServer.stopServer();
		} catch (Exception e) {}
	}

	/**
	 * returns {@link MessagePublisher}
	 * 
	 * @param publisher - ID of {@link Element}
	 * @return
	 */
	public MessagePublisher getPublisher(String publisher) {
		return publishers.get(publisher);
	}

	/**
	 * returns {@link MessageReceiver}
	 * 
	 * @param receiver - ID of {@link Element}
	 * @return
	 */
	public MessageReceiver getReceiver(String receiver) {
		return receivers.get(receiver);
	}
	
	/**
	 * returns next generated message id
	 * 
	 * @return
	 */
	public synchronized String getNextId() {
		return idGenerator.getNextId();
	}
	
	/**
	 * returns routes
	 * 
	 * @return
	 */
	public synchronized Map<String, Route> getRoutes() {
		return elementManager.getRoutes();
	}
}
