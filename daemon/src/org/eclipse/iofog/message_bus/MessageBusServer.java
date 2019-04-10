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

import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.*;
import org.hornetq.api.core.client.ClientSession.QueueQuery;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.HornetQServers;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HornetQ server
 * 
 * @author saeid
 *
 */
public class MessageBusServer {

	public static final Object messageBusSessionLock = new Object();
	private static final String MODULE_NAME = "Message Bus Server";
	private ClientSessionFactory sf;
	private HornetQServer server;
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
		ClientProducer producer = producers.get(name);
		return producer == null || producer.isClosed();
	}
	
	boolean isConsumerClosed(String name) {
		ClientConsumer consumer = consumers.get(name); 
		return consumer == null || consumer.isClosed();
	}
	
	/**
	 * starts HornetQ server 
	 * 
	 * @throws Exception
	 */
	void startServer() throws Exception {
		LoggingService.logInfo(MODULE_NAME, "starting...");
		AddressSettings addressSettings = new AddressSettings();
		long memoryLimit = (long) (Configuration.getMemoryLimit() * 1_000_000);
		addressSettings.setMaxSizeBytes(memoryLimit);
		addressSettings.setAddressFullMessagePolicy(AddressFullMessagePolicy.DROP);
		String workingDirectory = Configuration.getDiskDirectory();

        org.hornetq.core.config.Configuration configuration = new ConfigurationImpl();
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
		connectionParams.put("port", 55555);
		connectionParams.put("host", "localhost");
		TransportConfiguration nettyConfig = new TransportConfiguration(NettyAcceptorFactory.class.getName(), connectionParams);

        HashSet<TransportConfiguration> transportConfig = new HashSet<>();
		transportConfig.add(nettyConfig);
        transportConfig.add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
        
		configuration.setAcceptorConfigurations(transportConfig);
		server = HornetQServers.newHornetQServer(configuration);
		server.start();

        serverLocator = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()));

        serverLocator.setUseGlobalPools(false);
        serverLocator.setScheduledThreadPoolMaxSize(10);
        serverLocator.setThreadPoolMaxSize(10);
        sf = serverLocator.createSessionFactory();
	}
	
	/**
	 * creates IOFog message queues, {@link ClientMessage} producer
	 * and {@link ClientSession}
	 * 
	 * @throws Exception
	 */
	void initialize() throws Exception {
		synchronized (messageBusSessionLock) {
			messageBusSession = sf.createSession(true, true, 0);
			QueueQuery queueQuery = messageBusSession.queueQuery(new SimpleString(Constants.ADDRESS));
			if (queueQuery.isExists())
				messageBusSession.deleteQueue(Constants.ADDRESS);
			queueQuery = messageBusSession.queueQuery(new SimpleString(Constants.COMMAND_LINE_ADDRESS));
			if (queueQuery.isExists())
				messageBusSession.deleteQueue(Constants.COMMAND_LINE_ADDRESS);
			messageBusSession.createQueue(Constants.ADDRESS, Constants.ADDRESS, false);
			messageBusSession.createQueue(Constants.COMMAND_LINE_ADDRESS, Constants.COMMAND_LINE_ADDRESS, false);

			commandlineProducer = messageBusSession.createProducer(Constants.COMMAND_LINE_ADDRESS);

			commandlineConsumer = messageBusSession.createConsumer(Constants.COMMAND_LINE_ADDRESS, String.format("receiver = '%s'", "iofog.commandline.command"));
			commandlineConsumer.setMessageHandler(new CommandLineHandler());
			messageBusSession.start();
		}
	}
	
	/**
	 * creates a new {@link ClientConsumer} for receiver {@link Microservice}
	 * 
	 * @param name - ID of {@link Microservice}
	 * @throws Exception
	 */
	void createConsumer(String name) throws Exception {
		if (consumers == null) {
			consumers = new ConcurrentHashMap<>();
		}

		ClientConsumer consumer;
		synchronized (messageBusSessionLock) {
			consumer = messageBusSession.createConsumer(Constants.ADDRESS, String.format("receiver = '%s'", name));
		}
		consumers.put(name, consumer);
	}
	
	/**
	 * returns {@link ClientConsumer} of a receiver {@link Microservice}
	 * 
	 * @param receiver - ID of {@link Microservice}
	 * @return {@link ClientConsumer}
	 */
	ClientConsumer getConsumer(String receiver) {
		if (consumers == null || !consumers.containsKey(receiver))
			try {
				createConsumer(receiver);
			} catch (Exception e) {
				return null;
			}
		return consumers.get(receiver);
	}
	
	/**
	 * removes {@link ClientConsumer} when a receiver {@link Microservice} has been removed
	 * 
	 * @param name - ID of {@link Microservice}
	 */
	void removeConsumer(String name) {
		if (consumers == null)
			return;
		consumers.remove(name);
	}
	
	/**
	 * creates a new {@link ClientProducer} for publisher {@link Microservice}
	 * 
	 * @param name - ID of {@link Microservice}
	 * @throws Exception
	 */
	void createProducer(String name) throws Exception {
		if (producers == null) {
			producers = new ConcurrentHashMap<>();
		}
		ClientProducer producer;
		synchronized (messageBusSessionLock) {
			producer = messageBusSession.createProducer(Constants.ADDRESS);
		}
		producers.put(name, producer);
	}
	
	/**
	 * returns {@link ClientProducer} of a publisher {@link Microservice}
	 * 
	 * @param publisher - ID of {@link Microservice}
	 * @return {@link ClientProducer}
	 */
	ClientProducer getProducer(String publisher) {
		if (producers == null || !producers.containsKey(publisher))
			try {
				createProducer(publisher);
			} catch (Exception e) {
				return null;
			}
		return producers.get(publisher);
	}
	
	/**
	 * removes {@link ClientConsumer} when a receiver {@link Microservice} has been removed
	 * 
	 * @param name - ID of {@link Microservice}
	 */
	void removeProducer(String name) {
		if (producers == null)
			return;
		producers.remove(name);
	}
	
	static ClientSession getSession() {
		return messageBusSession;
	}
	
	static ClientProducer getCommandlineProducer() {
		return commandlineProducer;
	}

	/**
	 * stops all consumers, producers and HornetQ server
	 * 
	 * @throws Exception
	 */
	void stopServer() throws Exception {
		LoggingService.logInfo(MODULE_NAME, "stopping...");
		if (consumers != null)
			consumers.forEach((key, value) -> {
				try {
					value.close();
				} catch (HornetQException e) {
					LoggingService.logInfo(MODULE_NAME, e.getMessage());
				}
			});
		if (commandlineConsumer != null)
			commandlineConsumer.close();
		if (producers != null)
			producers.forEach((key, value) -> {
				try {
					value.close();
				} catch (HornetQException e) {
					LoggingService.logInfo(MODULE_NAME, e.getMessage());
				}
			});
		if (serverLocator != null)
			serverLocator.close();
		if (sf != null)
			sf.close();
		if (server != null)
			server.stop();
		LoggingService.logInfo(MODULE_NAME, "stopped");
	}

	/**
	 * sets memory usage limit of HornetQ server
	 * 
	 */
	void setMemoryLimit() {
		AddressSettings addressSettings = new AddressSettings();
		long memoryLimit = (long) (Configuration.getMemoryLimit() * 1_000_000);
		addressSettings.setMaxSizeBytes(memoryLimit);
		addressSettings.setAddressFullMessagePolicy(AddressFullMessagePolicy.DROP);

		server.getAddressSettingsRepository().addMatch(Constants.ADDRESS, addressSettings);
	}
}
