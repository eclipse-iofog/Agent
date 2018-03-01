package org.eclipse.iofog.network;

import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.net.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import static org.eclipse.iofog.command_line.util.CommandShellExecutor.executeCommand;

/**
 * Created by ekrylovich
 * on 2/8/18.
 */
public class IOFogNetworkInterface {
	private static final String MODULE_NAME = "IOFogNetworkInterface";

	private static final String NETWORK_BASH_COMMAND = "route | grep '^default' | grep -o '[^ ]*$'";

	/**
	 * returns IPv4 host address of IOFog network interface
	 *
	 * @return {@link Inet4Address}
	 * @throws Exception
	 */
	public static String getCurrentIpAddress() {
		Optional<InetAddress> inetAddress = getLocalIp();
		return inetAddress.map(InetAddress::getHostAddress).orElse("");
	}

	private static Optional<InetAddress> getLocalIp(){
		Optional<InetAddress> inetAddress = Optional.empty();
		try {
			inetAddress = Optional.of(getInetAddress());
		} catch (SocketException exp) {
			LoggingService.logWarning(MODULE_NAME, "Unable to find the IP address of the machine running ioFog: " + exp.getMessage());
		}
		return inetAddress;
	}

	/**
	 * returns IPv4 address of IOFog network interface
	 *
	 * @return {@link Inet4Address}
	 * @throws Exception
	 */
	public static InetAddress getInetAddress() throws SocketException {
		final NetworkInterface interfaceByName = NetworkInterface.getByName(getNetworkInterface());
		if (interfaceByName != null) {
			final Enumeration<InetAddress> ipAddresses = interfaceByName
					.getInetAddresses();
			while (ipAddresses.hasMoreElements()) {
				InetAddress address = ipAddresses.nextElement();
				if (address instanceof Inet4Address) {
					return address;
				}
			}
		}
		throw new ConnectException(String.format("unable to get ip address \"%s\"", getNetworkInterface()));
	}


	public static String getNetworkInterface(){
		final String configNetworkInterface = Configuration.getNetworkInterface();
		return configNetworkInterface.equals("dynamic") ? getOSNetworkInterface() : configNetworkInterface;
	}



	private static String getOSNetworkInterface(){
		final CommandShellResultSet<List<String>, List<String>> interfaces = executeCommand(NETWORK_BASH_COMMAND);
		return !interfaces.getError().isEmpty() || interfaces.getValue().isEmpty() ?
			"enp0s25" :
			interfaces.getValue().get(0);
	}
}
