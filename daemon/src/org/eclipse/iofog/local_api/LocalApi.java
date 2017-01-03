package org.eclipse.iofog.local_api;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.iofog.element.ElementManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.Constants.ModulesStatus;
import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * Local api point of start using iofog. 
 * Get and update the configuration for local api module.
 * @author ashita
 * @since 2016
 */
public class LocalApi implements Runnable {

	private final String MODULE_NAME = "Local API";
	private static LocalApi instance = null;
	public boolean isSeverStarted = false; 
	private LocalApiServer server;

	private LocalApi() {

	} 

	/**
	 * Instantiate local api - singleton
	 * @param None
	 * @return LocalApi
	 */
	public static LocalApi getInstance(){
		if (instance == null) {
			synchronized (LocalApi.class) {
				if(instance == null){
					instance = new LocalApi();
					LoggingService.logInfo("LOCAL API ","Local Api Instantiated");
				}
			}
		}
		return instance;
	}

	/**
	 * Stop local api server
	 * @param None
	 * @return void
	 */
	public void stopServer() throws Exception {
		server.stop();
	}


	/**
	 * Start local api server
	 * Instantiate websocket map and configuration map
	 * @param None
	 * @return void
	 */
	@Override
	public void run() {
		StatusReporter.setSupervisorStatus().setModuleStatus(Constants.LOCAL_API, ModulesStatus.STARTING);

		WebSocketMap.getInstance();
		ConfigurationMap.getInstance();

		try {
			StatusReporter.setLocalApiStatus().setCurrentIpAddress(Orchestrator.getInetAddress());
		} catch (Exception e2) {
			LoggingService.logWarning(MODULE_NAME, "Unable to find the IP address of the machine running ioFog: " + e2.getMessage());
		}

		StatusReporter.setLocalApiStatus().setOpenConfigSocketsCount(WebSocketMap.controlWebsocketMap.size());
		StatusReporter.setLocalApiStatus().setOpenMessageSocketsCount(WebSocketMap.messageWebsocketMap.size());

		retrieveContainerConfig();

		server = new LocalApiServer();
		try {
			server.start();
			isSeverStarted = true;
			
		} catch (Exception e) {
			try {
				stopServer();
				isSeverStarted = false;
			} catch (Exception e1) {
				LoggingService.logWarning(MODULE_NAME, "unable to start local api server: " + e1.getMessage());
				StatusReporter.setSupervisorStatus().setModuleStatus(Constants.LOCAL_API, ModulesStatus.STOPPED);
				return;
			}

			LoggingService.logWarning(MODULE_NAME, "unable to start local api server: " + e.getMessage());
			StatusReporter.setSupervisorStatus().setModuleStatus(Constants.LOCAL_API, ModulesStatus.STOPPED);
			return;
		}

	}

	/**
	 * Get the containers configuration and store it.
	 * @param None
	 * @return void
	 */
	public void retrieveContainerConfig() {
		try {
			ConfigurationMap.containerConfigMap = ElementManager.getInstance().getConfigs();
			LoggingService.logInfo(MODULE_NAME, "Container configuration retrieved");
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "unable to retrieve containers configuration: " + e.getMessage());
		}	  
	}

	/**
	 * Update the containers configuration and store it.
	 * @param None
	 * @return void
	 */
	public void updateContainerConfig(){
		try {
			ConfigurationMap.containerConfigMap = ElementManager.getInstance().getConfigs();
			LoggingService.logInfo(MODULE_NAME, "Container configuration updated");
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "unable to update containers configuration: " + e.getMessage());
		}
	}

	/**
	 * Initiate the real-time control signal when the cofiguration changes.
	 * Called by field-agtent.
	 * @param None
	 * @return void
	 */
	public void update(){
		try {
			StatusReporter.setLocalApiStatus().setCurrentIpAddress(Orchestrator.getInetAddress());
		} catch (Exception e2) {
			LoggingService.logWarning(MODULE_NAME, "Unable to find the IP address of the machine running ioFog: " + e2.getMessage());
		}

		Map<String, String> oldConfigMap = new HashMap<String, String>();
		oldConfigMap.putAll(ConfigurationMap.containerConfigMap);
		updateContainerConfig();
		Map<String, String> newConfigMap = new HashMap<String, String>();
		newConfigMap.putAll(ConfigurationMap.containerConfigMap);
		ControlWebsocketHandler handler = new ControlWebsocketHandler();
		try {
			handler.initiateControlSignal(oldConfigMap, newConfigMap);
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "Unable to complete the control signal sending " + e.getMessage());
		}
	}
}