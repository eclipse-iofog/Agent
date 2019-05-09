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
package org.eclipse.iofog;

import io.sentry.Sentry;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import static org.eclipse.iofog.utils.CmdProperties.getVersion;


import static java.nio.charset.StandardCharsets.UTF_8;

public class Daemon {
    private static final String MODULE_NAME = "MAIN_DAEMON";

    private static final String LOCAL_API_ENDPOINT = "http://localhost:54321/v2/commandline";

    /**
     * check if another instance of iofog is running
     *
     * @return boolean
     */
    private static boolean isAnotherInstanceRunning() {

        try {
            URL url = new URL(LOCAL_API_ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.getResponseCode();
            conn.disconnect();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * send command-line parameters to ioFog daemon
     *
     * @param args - parameters
     */
    private static boolean sendCommandlineParameters(String... args) {
        if (args[0].equals("stop")) {
            System.out.println("Stopping iofog service...");
            System.out.flush();
        }

        try {
            StringBuilder params = new StringBuilder("{\"command\":\"");
            for (String arg : args) {
                params.append(arg).append(" ");
            }
            params = new StringBuilder(params.toString().trim() + "\"}");
            byte[] postData = params.toString().trim().getBytes(UTF_8);

            String accessToken = fetchAccessToken();

            URL url = new URL(LOCAL_API_ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "text/plain");
            conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
            conn.setRequestProperty("Authorization", accessToken);
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData);
            }

            if (conn.getResponseCode() != 200) {
                return false;
            }
            StringBuilder result = new StringBuilder();

            try (BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream()),
                    UTF_8))) {
                String output;
                while ((output = br.readLine()) != null) {
                    result.append(output);
                }
            }


            conn.disconnect();

            System.out.println(result.toString().replace("\\n", "\n"));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * creates and grants permission to daemon files directory
     */
    private static void setupEnvironment() {
        final File daemonFilePath = new File(Constants.VAR_RUN);
        daemonFilePath.mkdirs();
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
        LoggingService.logInfo(MODULE_NAME, "Configuration loaded.");

    }

    /**
     * ports standard output to null
     */
    private static void outToNull() {
        Constants.systemOut = System.out;
        try {
            if (!Configuration.debugging) {
                System.setOut(new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) {
                        // DO NOTHING
                    }
                }, false, UTF_8.name()));

                System.setErr(new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) {
                        // DO NOTHING
                    }
                }, false, UTF_8.name()));
            }
        } catch (UnsupportedEncodingException ex) {
            LoggingService.logInfo(MODULE_NAME, ex.getMessage());
        }
    }

    public static void main(String[] args) throws ParseException {
        Sentry.init("https://0c6c3531b8b5402287e2688281bbbf06@sentry.io/1378607");

        Sentry.getContext().addExtra("version", getVersion());

        try {
            Configuration.load();

            setupEnvironment();

            if (args == null || args.length == 0)
                System.exit(0);

            if (isAnotherInstanceRunning()) {
                if (args[0].equals("start")) {
                    System.out.println("ioFog Agent is already running.");
                } else if (args[0].equals("stop")) {
                    sendCommandlineParameters(args);
                }
            } else if (args[0].equals("start")) {
                startLoggingService();

                outToNull();

                Configuration.setupSupervisor();

                System.setOut(Constants.systemOut);
            }
        } catch (Exception e) {
            Sentry.capture(e);
        }
    }

    private static String fetchAccessToken() {
        String line = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(Constants.LOCAL_API_TOKEN_PATH))) {
            line = reader.readLine();
        } catch (IOException e) {
            System.out.println("Local API access token is missing, try to re-install Agent.");
        }

        return line;
    }

}