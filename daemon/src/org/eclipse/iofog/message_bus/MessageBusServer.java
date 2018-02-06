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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.iofog.element.Element;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSession.QueueQuery;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.HornetQServers;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;

/**
 * HornetQ server
 * 
 * @author saeid
 *
 */
public class MessageBusServer {
	
	private final String MODULE_NAME = "Message Bus Server";
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
        configuration.getAddressesSettings().put(Constants.address, addressSettings);
        
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
		messageBusSession = sf.createSession(true, true, 0);
		QueueQuery queueQuery = messageBusSession.queueQuery(new SimpleString(Constants.address));
		if (queueQuery.isExists())
			messageBusSession.deleteQueue(Constants.address);
		queueQuery = messageBusSession.queueQuery(new SimpleString(Constants.commandlineAddress));
		if (queueQuery.isExists())
			messageBusSession.deleteQueue(Constants.commandlineAddress);
		messageBusSession.createQueue(Constants.address, Constants.address, false);
		messageBusSession.createQueue(Constants.commandlineAddress, Constants.commandlineAddress, false);

		commandlineProducer = messageBusSession.createProducer(Constants.commandlineAddress);
		
		commandlineConsumer = messageBusSession.createConsumer(Constants.commandlineAddress, String.format("receiver = '%s'", "iofog.commandline.command"));
		commandlineConsumer.setMessageHandler(new CommandLineHandler());
		messageBusSession.start();
	}
	
	/**
	 * creates a new {@link ClientConsumer} for receiver {@link Element}
	 * 
	 * @param name - ID of {@link Element}
	 * @throws Exception
	 */
	void createCosumer(String name) throws Exception {
		if (consumers == null)
			consumers = new ConcurrentHashMap<>();

		ClientConsumer consumer = messageBusSession.createConsumer(Constants.address, String.format("receiver = '%s'", name));
		consumers.put(name, consumer);
	}
	
	/**
	 * returns {@link ClientConsumer} of a receiver {@link Element}
	 * 
	 * @param receiver - ID of {@link Element}
	 * @return {@link ClientConsumer}
	 */
	ClientConsumer getConsumer(String receiver) {
		if (consumers == null || !consumers.containsKey(receiver))
			try {
				createCosumer(receiver);
			} catch (Exception e) {
				return null;
			}
		return consumers.get(receiver);
	}
	
	/**
	 * removes {@link ClientConsumer} when a receiver {@link Element} has been removed
	 * 
	 * @param name - ID of {@link Element}
	 */
	void removeConsumer(String name) {
		if (consumers == null)
			return;
		consumers.remove(name);
	}
	
	/**
	 * creates a new {@link ClientProducer} for publisher {@link Element}
	 * 
	 * @param name - ID of {@link Element}
	 * @throws Exception
	 */
	void createProducer(String name) throws Exception {
		if (producers == null)
			producers = new ConcurrentHashMap<>();
		ClientProducer producer = messageBusSession.createProducer(Constants.address);
		producers.put(name, producer);
	}
	
	/**
	 * returns {@link ClientProducer} of a publisher {@link Element} 
	 * 
	 * @param publisher - ID of {@link Element}
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
	 * removes {@link ClientConsumer} when a receiver {@link Element} has been removed
	 * 
	 * @param name - ID of {@link Element}
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

		server.getAddressSettingsRepository().addMatch(Constants.address, addressSettings);
	}
}
