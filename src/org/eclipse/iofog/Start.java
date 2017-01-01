package org.eclipse.iofog;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.iofog.command_line.CommandLineParser;
import org.eclipse.iofog.supervisor.Supervisor;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.configuration.ConfigurationItemException;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;

public class Start {

	private static ClientSessionFactory sf = null;

	/**
	 * check if another instance of iofog is running
	 *
	 * @return boolean
	 */
	private static boolean isAnotherInstanceRunning() {
		Map<String, Object> connectionParams = new HashMap<>();
		connectionParams.put("port", 55555);
		connectionParams.put("host", "localhost");

		ServerLocator serverLocator = HornetQClient.createServerLocatorWithoutHA(
				new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams));
		try {
			sf = serverLocator.createSessionFactory();
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	/**
	 * send command-line parameters to ioFog daemon
	 * 
	 * @param args - parameters
	 *
	 */
	private static void sendCommandlineParameters(String... args) {
		if (args[0].equals("stop")) {
			System.out.println("Stopping iofog service...");
			System.out.flush();
		}

		String command = "";
		for (String str : args)
			command += str + " ";

		ClientSession session = null;
		try {
			session = sf.createSession();
			ClientConsumer consumer = session.createConsumer(Constants.commandlineAddress,
					"receiver = 'iofog.commandline.response'");
			ClientProducer producer = session.createProducer(Constants.commandlineAddress);
			session.start();

			ClientMessage received = consumer.receiveImmediate();
			while (received != null) {
				received.acknowledge();
				received = consumer.receiveImmediate();
			}

			ClientMessage message = session.createMessage(false);
			message.putStringProperty("command", command);
			message.putObjectProperty("receiver", "iofog.commandline.command");
			producer.send(message);
			if (args[0].equals("stop"))
				System.exit(0);
			received = consumer.receive();
			received.acknowledge();
			String response = received.getStringProperty("response");
			System.out.println(response);

			producer.close();
			consumer.close();
		} catch (Exception e) {
			// DO NOTHING
		} finally {
			if (sf != null) {
				sf.close();
			}
		}
		System.exit(0);
	}

	/**
	 * creates and grants permission to daemon files directory
	 */
	private static void setupEnvironment() {
		final File daemonFilePath = new File(Constants.VAR_RUN);
		if (!daemonFilePath.exists()) {
			try {
				daemonFilePath.mkdirs();

				UserPrincipalLookupService lookupservice = FileSystems.getDefault().getUserPrincipalLookupService();
				final GroupPrincipal group = lookupservice.lookupPrincipalByGroupName("iofog");
				Files.getFileAttributeView(daemonFilePath.toPath(), PosixFileAttributeView.class,
						LinkOption.NOFOLLOW_LINKS).setGroup(group);
				Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwx---");
				Files.setPosixFilePermissions(daemonFilePath.toPath(), perms);
			} catch (Exception e) {
			}
		}

	}

	/**
	 * loads config.xml
	 */
	private static void loadConfiguration() {
		try {
			Configuration.loadConfig();
		} catch (ConfigurationItemException e) {
			System.out.println("invalid configuration item(s).");
			System.out.println(e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			System.out.println("error accessing " + Constants.CONFIG_DIR);
			System.exit(1);
		}
	}

	/**
	 * starts logging service
	 */
	private static void startLoggingService() {
		try {
			LoggingService.setupLogger();
		} catch (IOException e) {
			System.out.println("Error starting logging service\n" + e.getMessage());
			System.exit(1);
		}
		LoggingService.logInfo("Main", "configuration loaded.");

	}

	/**
	 * ports standard output to null
	 */
	private static void outToNull() {
		Constants.systemOut = System.out;
		if (!Configuration.debugging) {
			System.setOut(new PrintStream(new OutputStream() {
				@Override
				public void write(int b) {
					// DO NOTHING
				}
			}));

			System.setErr(new PrintStream(new OutputStream() {
				@Override
				public void write(int b) {
					// DO NOTHING
				}
			}));
		}
	}

	public static void main(String[] args) throws ParseException {
		System.out.println(args);
		
		loadConfiguration();

		System.out.println(Configuration.getLogDiskDirectory());
		
		setupEnvironment();
		
		System.out.println("AFTER SETUP");

		if (args == null || args.length == 0)
			args = new String[] { "help" };

		if (isAnotherInstanceRunning()) {
			if (args[0].equals("stop") && (args.length != 2 || !args[1].equals("S"))) {
				System.out.println("Enter \"service iofog stop\"");
				System.exit(0);
			}

			if (args[0].equals("start")) {
				System.out.println("iofog is already running.");
				sf.close();
				System.exit(1);
			}

			sendCommandlineParameters(args);
		}

		if (args[0].equals("help") || args[0].equals("--help") || args[0].equals("-h") || args[0].equals("-?")
				|| args[0].equals("version") || args[0].equals("--version") || args[0].equals("-v")) {
			System.out.println(CommandLineParser.parse(args[0]));
			System.out.flush();
			System.exit(0);
		} else if (!args[0].equals("start")) {
				System.out.println("iofog is not running.");
				System.out.flush();
				System.exit(1);
		}
		
		if(args[0].equals("start") && (args.length != 2 || !args[1].equals("S"))) {
			System.out.println("Enter \"service iofog start\"");
			System.out.flush();
			System.exit(1);
		}
			
		startLoggingService();

		System.out.println("AFTER LOG");

		outToNull();

		LoggingService.logInfo("Main", "starting supervisor");
		Supervisor supervisor = new Supervisor();
		try {
			supervisor.start();
		} catch (Exception e) {}

		System.setOut(Constants.systemOut);
	}

}