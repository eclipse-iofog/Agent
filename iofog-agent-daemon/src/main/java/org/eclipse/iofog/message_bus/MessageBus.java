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
package org.eclipse.iofog.message_bus;

import io.netty.channel.ChannelHandlerContext;
import org.apache.qpid.jms.JmsConnectionRemotelyClosedException;
import org.apache.qpid.jms.exceptions.JmsConnectionClosedException;
import org.apache.qpid.jms.exceptions.JmsConnectionFailedException;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.local_api.WebSocketMap;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
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
	
	final static String MODULE_NAME = "Message Bus";

	private MessageBusServer messageBusServer;
	private Map<String, Route> routes;
	private Map<String, MessagePublisher> publishers;
	private Map<String, MessageReceiver> receivers;
	private MessageIdGenerator idGenerator;
	private static MessageBus instance;
	private MicroserviceManager microserviceManager;
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
	 * enables real-time {@link Message} receiving of an {@link Microservice}
	 * 
	 * @param receiver - ID of {@link Microservice}
	 */
	public synchronized void enableRealTimeReceiving(String receiver) {
		logInfo("Starting enable real time receiving");
		MessageReceiver rec = receiver != null ? receivers.get(receiver) : null;
		if (rec == null)
			return;
		rec.enableRealTimeReceiving();
		logInfo("Finishing enable real time receiving");
	}

	/**
	 * disables real-time {@link Message} receiving of an {@link Microservice}
	 * 
	 * @param receiver - ID of {@link Microservice}
	 */
	public synchronized void disableRealTimeReceiving(String receiver) {
		logInfo("Starting disable real time receiving");
		MessageReceiver rec = receiver != null ? receivers.get(receiver) : null;
		if (rec == null)
			return;
		rec.disableRealTimeReceiving();
		logInfo("Finishing disable real time receiving");
	}

	/**
	 * initialize list of {@link Message} publishers and receivers
	 * 
	 */
	private void init() throws Exception {
		logInfo("Starting initialization of message bus publisher and receiver");
		lastSpeedMessageCount = 0;
		lastSpeedTime = System.currentTimeMillis();
		routes = microserviceManager.getRoutes();
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
						logError(MODULE_NAME,
								new AgentSystemException("unable to start publisher module :" + publisher, e));
					}
					publishers.put(publisher, new MessagePublisher(publisher, route, messageBusServer.getProducer(publisher)));
			});

		List<Microservice> microservices = microserviceManager.getLatestMicroservices();
		microservices.forEach(microservice -> {
			if (!microservice.isConsumer()) {
				return;
			}

			String id = microservice.getMicroserviceUuid();
			try {
				messageBusServer.createConsumer(id);
				MessageReceiver messageReceiver = new MessageReceiver(id, messageBusServer.getConsumer(id));
				receivers.put(id, messageReceiver);

				Map<String, ChannelHandlerContext> messageSocketMap = WebSocketMap.getMessageWebsocketMap();
				if (messageSocketMap.containsKey(id)) {
					messageReceiver.enableRealTimeReceiving();
				}
			} catch (Exception e) {
				logError(MODULE_NAME,
						new AgentSystemException("unable to start receiver module " + id, e));
			}
		});

		messageBusServer.setExceptionListener(new MessageBusExceptionListener(messageBusServer, publishers, receivers, routes, startServer));

		logInfo("Finished initialization of message bus publisher and receiver");
	}
	
	/**
	 * calculates the average speed of {@link Message} moving through ioFog
	 * 
	 */
	private final Runnable calculateSpeed = () -> {
		while (true) {
			try {
				logInfo("calculating message processing speed");
				Thread.sleep(Configuration.getSpeedCalculationFreqMinutes() * 60 * 1000);

				logInfo("Start calculating message processing speed");

				long now = System.currentTimeMillis();
				long msgs = StatusReporter.getMessageBusStatus().getProcessedMessages();

				float speed = ((float)(msgs - lastSpeedMessageCount)) / ((now - lastSpeedTime) / 1000f);
				StatusReporter.setMessageBusStatus().setAverageSpeed(speed);
				lastSpeedMessageCount = msgs;
				lastSpeedTime = now;
			} catch (Exception exp) {
				logError(MODULE_NAME,
						new AgentSystemException("unable to calculate message processing speed", exp));
			}
			logInfo("Finished calculating message processing speed");
		}
	};

	/**
	 * updates routing, list of publishers and receivers
	 * Field Agent calls this method when any changes applied
	 * 
	 */
	public void update() {
		logInfo("Start update routes, list of publishers and receivers");
		synchronized (updateLock) {
			Map<String, Route> newRoutes = microserviceManager.getRoutes();
			List<String> newPublishers = new ArrayList<>();
			List<String> newReceivers = new ArrayList<>();
			
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

			publishers.forEach((key, value) -> {
				if (!newPublishers.contains(key)) {
					value.close();
					try {
						messageBusServer.removeProducer(key);
					} catch (Exception e) { }
				} else {
					value.updateRoute(newRoutes.get(key));
				}
			});
			publishers.entrySet().removeIf(entry -> !newPublishers.contains(entry.getKey()));
			publishers.putAll(
					newPublishers.stream()
					.filter(publisher -> !publishers.containsKey(publisher))
					.collect(Collectors.toMap(publisher -> publisher, 
							publisher -> new MessagePublisher(publisher, newRoutes.get(publisher), messageBusServer.getProducer(publisher)))));

			receivers.forEach((key, value) -> {
				if (!newReceivers.contains(key)) {
					value.close();
					try {
						messageBusServer.removeConsumer(key);
					} catch (Exception e) { }
				}
			});
			receivers.entrySet().removeIf(entry -> !newReceivers.contains(entry.getKey()));
			receivers.putAll(
					newReceivers.stream()
					.filter(receiver -> !receivers.containsKey(receiver))
					.collect(Collectors.toMap(receiver -> receiver, 
							receiver -> new MessageReceiver(receiver, messageBusServer.getConsumer(receiver)))));

			routes = newRoutes;

			List<Microservice> latestMicroservices = microserviceManager.getLatestMicroservices();
			Map<String, Long> publishedMessagesPerMicroservice = StatusReporter.getMessageBusStatus().getPublishedMessagesPerMicroservice();
			publishedMessagesPerMicroservice.keySet().removeIf(key -> !microserviceManager.microserviceExists(latestMicroservices, key));
			latestMicroservices.forEach(e -> {
				if (!publishedMessagesPerMicroservice.keySet().contains(e.getMicroserviceUuid())) {
					publishedMessagesPerMicroservice.put(e.getMicroserviceUuid(), 0L);
				}
			});
		}
		logInfo("Finished update routes, list of publishers and receivers");
	}
	
	/**
	 * sets  memory usage limit of ActiveMQ
	 * {@link Configuration} calls this method when any changes applied
	 * 
	 */
	public void instanceConfigUpdated() {
		// TODO: Set router address if changed
	}

	private Runnable startServer = new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					logInfo("STARTING MESSAGE BUS SERVER");
					messageBusServer.startServer();
					messageBusServer.initialize();

					logInfo("MESSAGE BUS SERVER STARTED");
					init();

					new Thread(calculateSpeed, Constants.MESSAGE_BUS_CALCULATE_SPEED).start();

					break;
				} catch (Exception e) {
					try {
						Thread.sleep(2000);
						messageBusServer.stopServer();
					} catch (Exception exp) {
					}
					logError("Error starting message bus module",
							new AgentSystemException("Error starting message bus module", e));
					StatusReporter.setSupervisorStatus().setModuleStatus(MESSAGE_BUS, STOPPED);
				}
			}
		}
	};


	/**
	 * starts Message Bus module
	 * 
	 */
	public void start() {
		microserviceManager = MicroserviceManager.getInstance();

		messageBusServer = new MessageBusServer();

		new Thread(startServer).start();
	}
	
	/**
	 * closes receivers and publishers and stops ActiveMQ server
	 * 
	 */
	public void stop() {
		logInfo("Start closing receivers and publishers and stops ActiveMQ server");
		for (MessageReceiver receiver : receivers.values()) 
			receiver.close();
		
		for (MessagePublisher publisher : publishers.values())
			publisher.close();
		try {
			messageBusServer.stopServer();
		} catch (Exception exp) {
			logError("Error closing receivers and publishers and stops ActiveMQ server", new AgentSystemException("Error closing receivers and publishers and stops ActiveMQ server", exp));
		}
		logInfo("Finished closing receivers and publishers and stops ActiveMQ server");
	}

	/**
	 * returns {@link MessagePublisher}
	 * 
	 * @param publisher - ID of {@link Microservice}
	 * @return
	 */
	public MessagePublisher getPublisher(String publisher) {
		return publishers.get(publisher);
	}

	/**
	 * returns {@link MessageReceiver}
	 * 
	 * @param receiver - ID of {@link Microservice}
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
		return microserviceManager.getRoutes();
	}

	public static class MessageBusExceptionListener implements ExceptionListener {
		private MessageBusServer messageBusServer;
		private Map<String, MessagePublisher> publishers;
		private Map<String, MessageReceiver> receivers;
		private Map<String, Route> routes;
		private Runnable startServer;

		public MessageBusExceptionListener(MessageBusServer messageBusServer, Map<String, MessagePublisher> publishers, Map<String, MessageReceiver> receivers, Map<String, Route> routes, Runnable startServer) {
			this.messageBusServer = messageBusServer;
			this.publishers = publishers;
			this.receivers = receivers;
			this.routes = routes;
			this.startServer = startServer;
		}

		@Override
		public void onException(JMSException exception) {
			if (exception instanceof JmsConnectionClosedException
					|| exception instanceof JmsConnectionFailedException
					|| exception instanceof JmsConnectionRemotelyClosedException) {
				LoggingService.logError("Message Bus", "Server is not active. restarting...", exception);

				try {
					Thread.sleep(2000);
					publishers.forEach((key, publisher) -> { publisher.close(); });
					receivers.forEach((key, receiver) -> { receiver.close(); });
					messageBusServer.stopServer();
				} catch (Exception e) {}

				new Thread(startServer).start();
			}
		}
	}
}
