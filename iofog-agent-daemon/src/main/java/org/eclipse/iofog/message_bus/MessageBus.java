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

import io.netty.channel.ChannelHandlerContext;
import org.apache.qpid.jms.JmsConnectionRemotelyClosedException;
import org.apache.qpid.jms.exceptions.JmsConnectionClosedException;
import org.apache.qpid.jms.exceptions.JmsConnectionFailedException;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.field_agent.enums.RequestType;
import org.eclipse.iofog.local_api.WebSocketMap;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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
	private Map<String, MessagePublisher> publishers = new ConcurrentHashMap<>();
	private Map<String, MessageReceiver> receivers = new ConcurrentHashMap<>();
	private MessageIdGenerator idGenerator = new MessageIdGenerator();;
	private static MessageBus instance;
	private MicroserviceManager microserviceManager;
	private final Object updateLock = new Object();
	private String routerHost;
	private int routerPort;
	private ReentrantLock messageBusLock = new ReentrantLock();

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
		logDebug("Starting enable real time receiving");
		MessageReceiver rec = receiver != null ? receivers.get(receiver) : null;
		if (rec == null)
			return;
		rec.enableRealTimeReceiving();
		logDebug("Finishing enable real time receiving");
	}

	/**
	 * disables real-time {@link Message} receiving of an {@link Microservice}
	 * 
	 * @param receiver - ID of {@link Microservice}
	 */
	public synchronized void disableRealTimeReceiving(String receiver) {
		logDebug("Starting disable real time receiving");
		MessageReceiver rec = receiver != null ? receivers.get(receiver) : null;
		if (rec == null)
			return;
		rec.disableRealTimeReceiving();
		logDebug("Finishing disable real time receiving");
	}

	/**
	 * initialize list of {@link Message} publishers and receivers
	 * 
	 */
	private void init() throws Exception {
		logInfo("Starting initialization of message bus publisher and receiver");
		lastSpeedMessageCount = 0;
		lastSpeedTime = System.currentTimeMillis();

		updatePublishersAndReceivers();

		messageBusServer.setExceptionListener(new MessageBusExceptionListener(messageBusServer, publishers, receivers, startServer));

		logInfo("Finished initialization of message bus publisher and receiver");
	}
	
	/**
	 * calculates the average speed of {@link Message} moving through ioFog
	 * 
	 */
	private final Runnable calculateSpeed = () -> {
		while (true) {
			try {
				Thread.sleep(Configuration.getSpeedCalculationFreqMinutes() * 60 * 1000);

				logDebug("Start calculating message processing speed");

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
			logDebug("Finished calculating message processing speed");
		}
	};

	private void updatePublishersAndReceivers() throws Exception {
		Map<String, Route> newRoutes = microserviceManager.getRoutes();
		List<String> newPublishers = new ArrayList<>();
		List<String> newReceivers = new ArrayList<>();

		for (Map.Entry<String, Route> entry: newRoutes.entrySet()) {
			if (entry.getValue() == null || entry.getValue().getReceivers() == null) {
				continue;
			}

			newPublishers.add(entry.getKey());
			for (String receiver: entry.getValue().getReceivers()) {
				newReceivers.add(receiver);
			}
		}

		Set<String> keys = publishers.keySet();
		for (String key: keys) {
			if (!newPublishers.contains(key)) {
				publishers.get(key).close();
				messageBusServer.removeProducer(key);
				publishers.remove(key);
			} else {
				MessagePublisher publisher = publishers.get(key);
				Route route = newRoutes.get(key);
				Route currentRoute = publisher.getRoute();
				if (!currentRoute.equals(route)) {
					messageBusServer.removeProducer(key);
					publisher.updateRoute(route, messageBusServer.getProducer(key, route.getReceivers()));
				}
			}
		}

		for (String newPublisher: newPublishers) {
			if (publishers.containsKey(newPublisher)) {
				continue;
			}
			Route route = newRoutes.get(newPublisher);
			MessagePublisher messagePublisher = new MessagePublisher(newPublisher, route, messageBusServer.getProducer(newPublisher, route.getReceivers()));
			publishers.put(newPublisher, messagePublisher);
		}

		Set<String> recs = receivers.keySet();
		for (String rec: recs) {
			if (newReceivers.contains(rec)) {
				continue;
			}
			receivers.get(rec).close();
			messageBusServer.removeConsumer(rec);
			publishers.remove(rec);
		}

		for (String newReceiver: newReceivers) {
			if (receivers.containsKey(newReceiver)) {
				continue;
			}
			MessageReceiver messageReceiver = new MessageReceiver(newReceiver, messageBusServer.getConsumer(newReceiver));
			receivers.put(newReceiver, messageReceiver);
		}

		routes = newRoutes;

		List<Microservice> latestMicroservices = microserviceManager.getLatestMicroservices();
		Map<String, Long> publishedMessagesPerMicroservice = StatusReporter.getMessageBusStatus().getPublishedMessagesPerMicroservice();
		publishedMessagesPerMicroservice.keySet().removeIf(key -> !microserviceManager.microserviceExists(latestMicroservices, key));

		for (Microservice microservice: latestMicroservices) {
			if (!publishedMessagesPerMicroservice.keySet().contains(microservice.getMicroserviceUuid())) {
				publishedMessagesPerMicroservice.put(microservice.getMicroserviceUuid(), 0L);
			}

			if (!microservice.isConsumer()) {
				continue;
			}

			String id = microservice.getMicroserviceUuid();
			MessageConsumer consumer = messageBusServer.getConsumer(id);
			if (consumer != null) {
				MessageReceiver messageReceiver = new MessageReceiver(id, consumer);
				receivers.put(id, messageReceiver);

				Map<String, ChannelHandlerContext> messageSocketMap = WebSocketMap.getMessageWebsocketMap();
				if (messageSocketMap.containsKey(id)) {
					messageReceiver.enableRealTimeReceiving();
				}
			} else {
				throw new Exception("Unable to create consumer " + id);
			}
		}
	}

	/**
	 * updates routing, list of publishers and receivers
	 * Field Agent calls this method when any changes applied
	 * 
	 */
	public void update() throws Exception {
		logDebug("Start update routes, list of publishers and receivers");
		synchronized (updateLock) {
			if (!messageBusServer.isConnected()) {
				messageBusServer.setConnected(false);
				new Thread(startServer).start();
				throw new JMSException("Not connected to router");
			}

			String tempRouterHost = routerHost;
			int tempRouterPort = routerPort;
			getRouterAddress();
			if (!tempRouterHost.equals(routerHost) || tempRouterPort != routerPort) {
				try {
					stop();
				} catch (Exception ex) {
					logError(MODULE_NAME, new AgentSystemException("unable to update router info", ex));
				} finally {
					messageBusServer.setConnected(false);
					new Thread(startServer).start();
					throw new JMSException("Not connected to router");
				}
			}

			updatePublishersAndReceivers();
		}
		logDebug("Finished update routes, list of publishers and receivers");
	}
	
	/**
	 * sets  memory usage limit of ActiveMQ
	 * {@link Configuration} calls this method when any changes applied
	 * 
	 */
	public void instanceConfigUpdated() {
		// TODO: Set router address if changed
	}

	private void getRouterAddress() throws Exception {
		Orchestrator orchestrator = new Orchestrator();
		JsonObject configs = orchestrator.request("config", RequestType.GET, null, null);
		routerHost = configs.getString("routerHost");
		routerPort = configs.getJsonNumber("routerPort").intValue();
	}

	public void startServer() throws Exception {
		logInfo("STARTING MESSAGE BUS SERVER");

		getRouterAddress();

		messageBusServer.startServer(routerHost, routerPort);
		messageBusServer.initialize();

		logInfo("MESSAGE BUS SERVER STARTED");
		Thread speedCalc = new Thread(calculateSpeed, Constants.MESSAGE_BUS_CALCULATE_SPEED);
		speedCalc.start();
	}

	private Runnable startServer = new Runnable() {
		@Override
		public void run() {
			messageBusLock.lock();

			if (messageBusServer.isConnected()) {
				messageBusLock.unlock();
				return;
			}

			try {
				startServer();
				init();

				messageBusServer.setConnected(true);
			} catch (Exception e) {
				messageBusServer.setConnected(false);
				try {
					Thread.sleep(2000);
					stop();
				} catch (Exception exp) {
				}
				logWarning("Error starting message bus module: " +
						new AgentSystemException(e.getMessage(), e));
				StatusReporter.setSupervisorStatus().setModuleStatus(MESSAGE_BUS, STOPPED);
				new Thread(startServer).start();
			} finally {
				messageBusLock.unlock();
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

		if (receivers != null) {
			for (MessageReceiver receiver : receivers.values()) {
				try {
					receiver.close();
				} catch (Exception e) {
					logError("Error closing receiver " + receiver.getName(), new AgentSystemException(e.getMessage(), e));
				}
			}
			receivers.clear();
		}

		if (publishers != null) {
			for (MessagePublisher publisher : publishers.values()) {
				try {
					publisher.close();
				} catch (Exception e) {
					logError("Error closing publisher " + publisher.getName(), new AgentSystemException(e.getMessage(), e));
				}
			}
			publishers.clear();
		}

		try {
			messageBusServer.stopServer();
		} catch (Exception exp) {
			logError("Error closing receivers and publishers and stops ActiveMQ server", new AgentSystemException(exp.getMessage(), exp));
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
		private Runnable startServer;

		public MessageBusExceptionListener(MessageBusServer messageBusServer, Map<String, MessagePublisher> publishers, Map<String, MessageReceiver> receivers, Runnable startServer) {
			this.messageBusServer = messageBusServer;
			this.publishers = publishers;
			this.receivers = receivers;
			this.startServer = startServer;
		}

		@Override
		public void onException(JMSException exception) {
			if (exception instanceof JmsConnectionClosedException
					|| exception instanceof JmsConnectionFailedException
					|| exception instanceof JmsConnectionRemotelyClosedException) {
				LoggingService.logError("Message Bus", "Server is not active. restarting...", exception);

				try {
					messageBusServer.setConnected(false);
					Thread.sleep(2000);
					publishers.forEach((key, publisher) -> publisher.close());
					receivers.forEach((key, receiver) -> receiver.close());
					messageBusServer.stopServer();
				} catch (Exception e) {}

				new Thread(startServer).start();
			}
		}
	}
}
