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

package org.eclipse.iofog.network;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.functional.Pair;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static org.eclipse.iofog.command_line.CommandLineConfigParam.NETWORK_INTERFACE;

/**
 * Created by ekrylovich
 * on 2/8/18.
 */
public class IOFogNetworkInterface {
	private static final String MODULE_NAME = "IOFogNetworkInterface";

    private static final String UNABLE_TO_GET_IP_ADDRESS = "Unable to get ip address. Please check network connection";
    private static String dockerBridgeInterfaceName = "bridge";

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
        } catch (SocketException | MalformedURLException exp) {
            LoggingService.logError(MODULE_NAME, "Unable to find the local IP address", new AgentSystemException(exp.getMessage(), exp));
        } catch (Exception e){
            LoggingService.logError(MODULE_NAME, "Unable to find the IP address of the machine running ioFog exception occurred", new AgentSystemException(e.getMessage(), e));
        }
        return inetAddress;
    }

    /**
     * returns IPv4 address of IOFog network interface
     *
     * @return {@link Inet4Address}
     * @throws Exception
     */
    public static InetAddress getInetAddress() throws SocketException, MalformedURLException {
        try {
            final Pair<NetworkInterface, InetAddress> connectedAddress = getNetworkInterface();
            if (connectedAddress != null) {
                return connectedAddress._2();
            }
        } catch (SocketException | MalformedURLException e){
            throw e;
        } catch (Exception e){
            throw e;
        }
        return null;
    }

    public static Pair<NetworkInterface, InetAddress> getNetworkInterface() throws MalformedURLException, SocketException {
        final String configNetworkInterface = Configuration.getNetworkInterface();
        if (NETWORK_INTERFACE.getDefaultValue().equals(configNetworkInterface)) {
            return getOSNetworkInterface();
        }

        try {
            URL controllerUrl = new URL(Configuration.getControllerUrl());
            NetworkInterface networkInterface = NetworkInterface.getByName(configNetworkInterface);
            return getConnectedAddress(controllerUrl, networkInterface);
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Unable to get Network Interface : " + ExceptionUtils.getFullStackTrace(e));
            throw e;
        }
    }

    private static void setDockerBridgeInterfaceName() throws Exception {
        // Docker-Java#listNetworksCmd() stucks forever on ARM when docker url is set to unix domain
        // Adding a timeout
        Runnable getDockerBridgeInterfaceName = new Thread(() -> {
            dockerBridgeInterfaceName = DockerUtil.getInstance().getDockerBridgeName();
        });

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future future = executor.submit(getDockerBridgeInterfaceName);
        executor.shutdown();

        try {
            future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Unable to set Docker Bridge Interface Name : " + ExceptionUtils.getFullStackTrace(e));
            dockerBridgeInterfaceName = null;
        }
    }

    private static Pair<NetworkInterface, InetAddress> getOSNetworkInterface() {
        try {
            setDockerBridgeInterfaceName();

            URL controllerUrl = new URL(Configuration.getControllerUrl());
            NetworkInterface dockerBridgeNetworkInterface = null;

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface: Collections.list(networkInterfaces)) {
                if (dockerBridgeInterfaceName != null && networkInterface.getName().equals(dockerBridgeInterfaceName)) {
                    dockerBridgeNetworkInterface = networkInterface;
                    continue;
                }

                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }

                Pair<NetworkInterface, InetAddress> connectedAddress = getConnectedAddress(controllerUrl, networkInterface);
                if (connectedAddress != null) {
                    return connectedAddress;
                }
            }

            if (dockerBridgeNetworkInterface != null) {
                return getConnectedAddress(controllerUrl, dockerBridgeNetworkInterface, false);
            }

            return null;
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Unable to Get OS Network Interface : " + ExceptionUtils.getFullStackTrace(e));
            return null;
        }
    }

    private static Pair<NetworkInterface, InetAddress> getConnectedAddress(URL controllerUrl, NetworkInterface networkInterface) {
        return getConnectedAddress(controllerUrl, networkInterface, true);
    }

    private static Pair<NetworkInterface, InetAddress> getConnectedAddress(URL controllerUrl, NetworkInterface networkInterface, boolean checkConnection) {
        int controllerPort = controllerUrl.getPort();
        String controllerHost = controllerUrl.getHost();
        Enumeration<InetAddress> nifAddresses = networkInterface.getInetAddresses();
        for (InetAddress nifAddress: Collections.list(nifAddresses)) {
            LoggingService.logInfo(MODULE_NAME, "Detected network interface: " + networkInterface.toString() + " And network address - hostAddress : "+ nifAddress.getHostAddress()  + " type of : " + nifAddress.getClass().getName());
            if (!(nifAddress instanceof Inet4Address)) {
                continue;
            }

            if (!checkConnection) {
                return Pair.of(networkInterface, nifAddress);
            }

            try {
                Socket soc = new java.net.Socket();
                soc.bind(new InetSocketAddress(nifAddress, 0));
                soc.connect(new InetSocketAddress(controllerHost, controllerPort), 1000);
                soc.close();
                return Pair.of(networkInterface, nifAddress);
            } catch (Exception e) {
                LoggingService.logWarning(MODULE_NAME, "Unable to Get Connected Address : " + ExceptionUtils.getFullStackTrace(e));
            }
        }

        return null;
    }
    public static String getHostName() {
        String hostname = "";
        try {
            InetAddress ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
        } catch (UnknownHostException e) {
            LoggingService.logWarning(MODULE_NAME, "Unable to get hostname : " + ExceptionUtils.getFullStackTrace(e));
        }
        return hostname;
    }
}