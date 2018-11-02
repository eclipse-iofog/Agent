/*
 * *******************************************************************************
 *  * Copyright (c) 2018 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog.network;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.net.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import static org.eclipse.iofog.command_line.CommandLineConfigParam.NETWORK_INTERFACE;
import static org.eclipse.iofog.command_line.util.CommandShellExecutor.executeCommand;

/**
 * Created by ekrylovich
 * on 2/8/18.
 */
public class IOFogNetworkInterface {
	private static final String MODULE_NAME = "IOFogNetworkInterface";

    private static final String NETWORK_BASH_COMMAND = "route | grep '^default' | grep -o '[^ ]*$'";

    private static final String NETWORK_POWERSHELL_COMMAND = "Get-NetAdapter -physical | where status -eq 'up' | select -ExpandProperty Name";
    private static final String POWERSHELL_GET_IP_BY_INTERFACE_NAME = "Get-NetIPConfiguration | select IPv4Address, InterfaceAlias | where InterfaceAlias -eq '%s' | select -ExpandProperty IPv4Address | select -ExpandProperty IPAddress";
    private static final String POWERSHELL_GET_ACTIVE_IP = "(Get-WmiObject -Class Win32_NetworkAdapterConfiguration | where {$_.DefaultIPGateway -ne $null}).IPAddress | select-object -first 1";

    private static final String NETWORK_MACOS_COMMAND = "netstat -rn | grep '^default' | grep -o '[^ ]*$'";
    private static final String UNABLE_TO_GET_IP_ADDRESS = "Unable to get ip address. Please check network connection";
	private static final String LOOPBACK = "127.0.0.1";

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

    private static Optional<InetAddress> getLocalIp() {
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
        if (SystemUtils.IS_OS_WINDOWS) {
            try {
            // TODO too slow, replace later   InetAddress addr = InetAddress.getByName(getWindowsIpByInterfaceName(getNetworkInterface()));
                InetAddress addr = InetAddress.getByName(getWindowsActiveIp());

                final NetworkInterface interfaceByIp = NetworkInterface.getByInetAddress(addr);
                return getInetAddressByInterface(interfaceByIp);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
            final NetworkInterface interfaceByName = NetworkInterface.getByName(getNetworkInterface());
            if (interfaceByName != null) {
               return getInetAddressByInterface(interfaceByName);
            }
        }

        throw new ConnectException(UNABLE_TO_GET_IP_ADDRESS);
    }

    private static InetAddress getInetAddressByInterface(NetworkInterface networkInterface) throws SocketException {
        final Enumeration<InetAddress> ipAddresses = networkInterface
                .getInetAddresses();
        while (ipAddresses.hasMoreElements()) {
            InetAddress address = ipAddresses.nextElement();
            if (address instanceof Inet4Address) {
                return address;
            }
        }

        throw new ConnectException(UNABLE_TO_GET_IP_ADDRESS);
    }


    public static String getNetworkInterface() {
        final String configNetworkInterface = Configuration.getNetworkInterface();
        return configNetworkInterface.equals(NETWORK_INTERFACE.getDefaultValue())
				? getOSNetworkInterface()
				: configNetworkInterface;
    }


    private static String getOSNetworkInterface() {
        String command = NETWORK_BASH_COMMAND;
        if (SystemUtils.IS_OS_WINDOWS) {
            command = NETWORK_POWERSHELL_COMMAND;
        } else if (SystemUtils.IS_OS_LINUX) {
            command = NETWORK_BASH_COMMAND;
        } else if (SystemUtils.IS_OS_MAC) {
            command = NETWORK_MACOS_COMMAND;
        }

        final CommandShellResultSet<List<String>, List<String>> interfaces = executeCommand(command);
        return !interfaces.getError().isEmpty() || interfaces.getValue().isEmpty() ?
                "not found" :
                interfaces.getValue().get(0);
    }

    private static String getWindowsIpByInterfaceName(String interfaceName) {
        String cmd = String.format(POWERSHELL_GET_IP_BY_INTERFACE_NAME, interfaceName);

        final CommandShellResultSet<List<String>, List<String>> interfaces = executeCommand(cmd);
        return !interfaces.getError().isEmpty() || interfaces.getValue().isEmpty() ?
				LOOPBACK :
                interfaces.getValue().get(0);
    }

    private static String getWindowsActiveIp() {
        final CommandShellResultSet<List<String>, List<String>> interfaces = executeCommand(POWERSHELL_GET_ACTIVE_IP);
        return !interfaces.getError().isEmpty() || interfaces.getValue().isEmpty() ?
                LOOPBACK :
                interfaces.getValue().get(0);
    }
}