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

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActiveMQ server
 * 
 * @author saeid
 *
 */
public class MessageBusServer {

	public static final Object messageBusSessionLock = new Object();
	private static final String MODULE_NAME = "Message Bus Server";
	private ClientSessionFactory sf;
	private ActiveMQServer server;
	private static ClientSession messageBusSession;
	private ClientConsumer commandlineConsumer;
	private static ClientProducer commandlineProducer;
	private Map<String, ClientConsumer> consumers;
	private Map<String, ClientProducer> producers;
	private ServerLocator serverLocator;
	
	boolean isServerActive() {
		return server.isActive();
	}

	boolean isMessageBusSessionClosed() {
		synchronized (messageBusSessionLock) {
			return messageBusSession.isClosed();
		}
	}
	
	boolean isProducerClosed(String name) {
		synchronized (messageBusSessionLock) {
			ClientProducer producer = producers.get(name);
			return producer == null || producer.isClosed();
		}
	}
	
	boolean isConsumerClosed(String name) {
		synchronized (messageBusSessionLock) {
			ClientConsumer consumer = consumers.get(name);
			return consumer == null || consumer.isClosed();
		}
	}
	
	/**
	 * starts ActiveMQ server
	 * 
	 * @throws Exception
	 */
	void startServer() throws Exception {
		LoggingService.logInfo(MODULE_NAME, "starting server");
		AddressSettings addressSettings = new AddressSettings();
		long memoryLimit = (long) (Configuration.getMemoryLimit() * 1_000_000);
		addressSettings.setMaxSizeBytes(memoryLimit);
		addressSettings.setAddressFullMessagePolicy(AddressFullMessagePolicy.DROP);
		String workingDirectory = Configuration.getDiskDirectory();

        org.apache.activemq.artemis.core.config.Configuration configuration = new ConfigurationImpl();
        configuration.setJournalDirectory(workingDirectory + "messages/journal");
        configuration.setCreateJournalDir(true);
		configuration.setJournalType(JournalType.NIO);
        configuration.setBindingsDirectory(workingDirectory + "messages/binding");
		configuration.setCreateBindingsDir(true);
		configuration.setPersistenceEnabled(false);
        configuration.setSecurityEnabled(false);
        configuration.setPagingDirectory(workingDirectory + "messages/paging");
        configuration.getAddressesSettings().put(Constants.ADDRESS, addressSettings);
        
		Map<String, Object> connectionParams = new HashMap<>();
		connectionParams.put(TransportConstants.PORT_PROP_NAME, 55555);
		connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
		TransportConfiguration nettyConfig = new TransportConfiguration(NettyAcceptorFactory.class.getName(), connectionParams);

        HashSet<TransportConfiguration> transportConfig = new HashSet<>();
		transportConfig.add(nettyConfig);
        transportConfig.add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
        
		configuration.setAcceptorConfigurations(transportConfig);
		EmbeddedActiveMQ embeddedActiveMQ = new EmbeddedActiveMQ();
		embeddedActiveMQ.setConfiguration(configuration);
		server = embeddedActiveMQ.start().getActiveMQServer();

        serverLocator = ActiveMQClient.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()));

        serverLocator.setUseGlobalPools(false);
        serverLocator.setScheduledThreadPoolMaxSize(10);
        serverLocator.setThreadPoolMaxSize(10);
        sf = serverLocator.createSessionFactory();
        LoggingService.logInfo(MODULE_NAME, "Finished starting server");
	}
	
	/**
	 * creates IOFog message queues, {@link ClientMessage} producer
	 * and {@link ClientSession}
	 * 
	 * @throws Exception
	 */
	void initialize() throws Exception {
		LoggingService.logInfo(MODULE_NAME, "starting initialization");
		synchronized (messageBusSessionLock) {
			messageBusSession = sf.createSession(true, true, 0);
			ClientSession.QueueQuery queueQuery = messageBusSession.queueQuery(new SimpleString(Constants.ADDRESS));
			if (queueQuery != null && queueQuery.isExists())
				messageBusSession.deleteQueue(Constants.ADDRESS);
			queueQuery = messageBusSession.queueQuery(new SimpleString(Constants.COMMAND_LINE_ADDRESS));
			if (queueQuery != null && queueQuery.isExists())
				messageBusSession.deleteQueue(Constants.COMMAND_LINE_ADDRESS);
			messageBusSession.createQueue(Constants.ADDRESS, Constants.ADDRESS, false);
			messageBusSession.createQueue(Constants.COMMAND_LINE_ADDRESS, Constants.COMMAND_LINE_ADDRESS, false);

			commandlineProducer = messageBusSession.createProducer(Constants.COMMAND_LINE_ADDRESS);

			commandlineConsumer = messageBusSession.createConsumer(Constants.COMMAND_LINE_ADDRESS, String.format("receiver = '%s'", "iofog.commandline.command"));
			commandlineConsumer.setMessageHandler(new CommandLineHandler());
			messageBusSession.start();
		}
		LoggingService.logInfo(MODULE_NAME, "Finished initialization");
	}
	
	/**
	 * creates a new {@link ClientConsumer} for receiver {@link Microservice}
	 * 
	 * @param name - ID of {@link Microservice}
	 * @throws Exception
	 */
	void createConsumer(String name) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "Starting create consumer");
		if (consumers == null) {
			consumers = new ConcurrentHashMap<>();
		}

		ClientConsumer consumer;
		synchronized (messageBusSessionLock) {
			consumer = messageBusSession.createConsumer(Constants.ADDRESS, String.format("receiver = '%s'", name));
		}
		consumers.put(name, consumer);
		LoggingService.logInfo(MODULE_NAME, "Finished create consumer");
	}
	
	/**
	 * returns {@link ClientConsumer} of a receiver {@link Microservice}
	 * 
	 * @param receiver - ID of {@link Microservice}
	 * @return {@link ClientConsumer}
	 */
	ClientConsumer getConsumer(String receiver) {
		LoggingService.logInfo(MODULE_NAME, "Start get consumer");
		if (consumers == null || !consumers.containsKey(receiver))
			try {
				createConsumer(receiver);
			} catch (Exception e) {
				return null;
			}
		LoggingService.logInfo(MODULE_NAME, "Finished get consumer");
		return consumers.get(receiver);
	}
	
	/**
	 * removes {@link ClientConsumer} when a receiver {@link Microservice} has been removed
	 * 
	 * @param name - ID of {@link Microservice}
	 */
	void removeConsumer(String name) {
		LoggingService.logInfo(MODULE_NAME, "Start remove consumer");
		if (consumers == null)
			return;
		LoggingService.logInfo(MODULE_NAME, "Finished remove consumer");
		consumers.remove(name);
	}
	
	/**
	 * creates a new {@link ClientProducer} for publisher {@link Microservice}
	 * 
	 * @param name - ID of {@link Microservice}
	 * @throws Exception
	 */
	void createProducer(String name) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "Start create Producer");
		if (producers == null) {
			producers = new ConcurrentHashMap<>();
		}
		ClientProducer producer;
		synchronized (messageBusSessionLock) {
			producer = messageBusSession.createProducer(Constants.ADDRESS);
		}
		LoggingService.logInfo(MODULE_NAME, "Finish create Producer");
		producers.put(name, producer);
	}
	
	/**
	 * returns {@link ClientProducer} of a publisher {@link Microservice}
	 * 
	 * @param publisher - ID of {@link Microservice}
	 * @return {@link ClientProducer}
	 */
	ClientProducer getProducer(String publisher) {
		LoggingService.logInfo(MODULE_NAME, "Start get Producer");
		if (producers == null || !producers.containsKey(publisher))
			try {
				createProducer(publisher);
			} catch (Exception e) {
				return null;
			}
		LoggingService.logInfo(MODULE_NAME, "Finish get Producer");
		return producers.get(publisher);
	}
	
	/**
	 * removes {@link ClientConsumer} when a receiver {@link Microservice} has been removed
	 * 
	 * @param name - ID of {@link Microservice}
	 */
	void removeProducer(String name) {
		LoggingService.logInfo(MODULE_NAME, "Start remove Producer");
		if (producers == null)
			return;
		producers.remove(name);
		LoggingService.logInfo(MODULE_NAME, "Finish remove Producer");
	}
	
	static ClientSession getSession() {
		return messageBusSession;
	}
	
	static ClientProducer getCommandlineProducer() {
		return commandlineProducer;
	}

	/**
	 * stops all consumers, producers and ActiveMQ server
	 * 
	 * @throws Exception
	 */
	void stopServer() throws Exception {
		LoggingService.logInfo(MODULE_NAME, "stopping server started");
		if (consumers != null)
			consumers.forEach((key, value) -> {
				try {
					value.close();
				} catch (ActiveMQException e) {
					LoggingService.logError(MODULE_NAME, "Error closing consumer",
							new AgentSystemException("Error closing consumer", e));
				}
			});
		if (commandlineConsumer != null) {
			try {
				commandlineConsumer.close();
			} catch (ActiveMQException e) {
				LoggingService.logError(MODULE_NAME, "Error closing commandlineConsumer",
						new AgentSystemException("Error closing commandlineConsumer", e));
			}
		}
		if (producers != null)
			producers.forEach((key, value) -> {
				try {
					value.close();
				} catch (ActiveMQException e) {
					LoggingService.logError(MODULE_NAME, "Error closing producer",
							new AgentSystemException("Error closing producer", e));
				}
			});
		if (serverLocator != null)
			serverLocator.close();
		if (sf != null)
			sf.close();
		if (server != null)
			server.stop();
		LoggingService.logInfo(MODULE_NAME, "stopped server");
	}

	/**
	 * sets memory usage limit of ActiveMQ server
	 * 
	 */
	void setMemoryLimit() {
		LoggingService.logInfo(MODULE_NAME, "Start set memory limit");
		AddressSettings addressSettings = new AddressSettings();
		long memoryLimit = (long) (Configuration.getMemoryLimit() * 1_000_000);
		addressSettings.setMaxSizeBytes(memoryLimit);
		addressSettings.setAddressFullMessagePolicy(AddressFullMessagePolicy.DROP);

		server.getAddressSettingsRepository().addMatch(Constants.ADDRESS, addressSettings);
		LoggingService.logInfo(MODULE_NAME, "Finished set memory limit");
	}
}
