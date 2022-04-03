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
package org.eclipse.iofog.field_agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.diagnostics.ImageDownloadManager;
import org.eclipse.iofog.diagnostics.strace.MicroserviceStraceData;
import org.eclipse.iofog.diagnostics.strace.StraceDiagnosticManager;
import org.eclipse.iofog.edge_resources.*;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.enums.RequestType;
import org.eclipse.iofog.local_api.LocalApi;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.microservice.*;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.proxy.SshConnection;
import org.eclipse.iofog.proxy.SshProxyManager;
import org.eclipse.iofog.pruning.DockerPruningManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.functional.Pair;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.*;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HttpMethod;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.netty.util.internal.StringUtil.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.rightPad;
import static org.eclipse.iofog.command_line.CommandLineConfigParam.*;
import static org.eclipse.iofog.resource_manager.ResourceManager.*;
import static org.eclipse.iofog.utils.CmdProperties.*;
import static org.eclipse.iofog.utils.Constants.*;
import static org.eclipse.iofog.utils.Constants.ControllerStatus.*;

/**
 * Field Agent module
 *
 * @author saeid
 */
public class FieldAgent implements IOFogModule {

    private final String MODULE_NAME = "Field Agent";
    private final String filesPath = SystemUtils.IS_OS_WINDOWS ? SNAP_COMMON + "./etc/iofog-agent/" : SNAP_COMMON + "/etc/iofog-agent/";

    private Orchestrator orchestrator;
    private SshProxyManager sshProxyManager;
    private long lastGetChangesList;
    private MicroserviceManager microserviceManager;
    private static FieldAgent instance;
    private boolean initialization;
    private boolean connected = false;
    private ReentrantLock provisioningLock = new ReentrantLock();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private ScheduledFuture<?> futureTask;
    private EdgeResourceManager edgeResourceManager;

    private FieldAgent() {
        lastGetChangesList = 0;
        initialization = true;
    }

    @Override
    public int getModuleIndex() {
        return FIELD_AGENT;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    public static FieldAgent getInstance() {
        if (instance == null) {
            synchronized (FieldAgent.class) {
                if (instance == null)
                    instance = new FieldAgent();
            }
        }
        return instance;
    }

    /**
     * creates IOFog status report
     *
     * @return Map
     */
    private JsonObject getFogStatus() {
    	logDebug("get Fog Status");
        return Json.createObjectBuilder()
                .add("daemonStatus", StatusReporter.getSupervisorStatus().getDaemonStatus().toString() == null ?
                        "UNKNOWN" : StatusReporter.getSupervisorStatus().getDaemonStatus().toString())
                .add("daemonOperatingDuration", StatusReporter.getSupervisorStatus().getOperationDuration())
                .add("daemonLastStart", StatusReporter.getSupervisorStatus().getDaemonLastStart())
                .add("memoryUsage", StatusReporter.getResourceConsumptionManagerStatus().getMemoryUsage())
                .add("diskUsage", StatusReporter.getResourceConsumptionManagerStatus().getDiskUsage())
                .add("cpuUsage", StatusReporter.getResourceConsumptionManagerStatus().getCpuUsage())
                .add("memoryViolation", StatusReporter.getResourceConsumptionManagerStatus().isMemoryViolation())
                .add("diskViolation", StatusReporter.getResourceConsumptionManagerStatus().isDiskViolation())
                .add("cpuViolation", StatusReporter.getResourceConsumptionManagerStatus().isCpuViolation())
                .add("systemAvailableDisk", StatusReporter.getResourceConsumptionManagerStatus().getAvailableDisk())
                .add("systemAvailableMemory", StatusReporter.getResourceConsumptionManagerStatus().getAvailableMemory())
                .add("systemTotalCpu", StatusReporter.getResourceConsumptionManagerStatus().getTotalCpu())
                .add("microserviceStatus", StatusReporter.getProcessManagerStatus().getJsonMicroservicesStatus() == null ?
                        Json.createObjectBuilder().add("status","UNKNOWN").build().toString() :
                        StatusReporter.getProcessManagerStatus().getJsonMicroservicesStatus())
                .add("repositoryCount", StatusReporter.getProcessManagerStatus().getRegistriesCount())
                .add("repositoryStatus", StatusReporter.getProcessManagerStatus().getJsonRegistriesStatus() == null ?
                        "UNKNOWN" : StatusReporter.getProcessManagerStatus().getJsonRegistriesStatus())
                .add("systemTime", StatusReporter.getStatusReporterStatus().getSystemTime())
                .add("lastStatusTime", StatusReporter.getStatusReporterStatus().getLastUpdate())
                .add("ipAddress", IOFogNetworkInterfaceManager.getInstance().getCurrentIpAddress() == null ?
                        "UNKNOWN" : IOFogNetworkInterfaceManager.getInstance().getCurrentIpAddress())
                .add("ipAddressExternal", Configuration.getIpAddressExternal() == null ?
                        "UNKNOWN" : Configuration.getIpAddressExternal())
                .add("processedMessages", StatusReporter.getMessageBusStatus().getProcessedMessages())
                .add("microserviceMessageCounts", StatusReporter.getMessageBusStatus().getJsonPublishedMessagesPerMicroservice() == null ?
                        "UNKNOWN" : StatusReporter.getMessageBusStatus().getJsonPublishedMessagesPerMicroservice())
                .add("messageSpeed", StatusReporter.getMessageBusStatus().getAverageSpeed())
                .add("lastCommandTime", StatusReporter.getFieldAgentStatus().getLastCommandTime())
                .add("tunnelStatus", StatusReporter.getSshManagerStatus().getJsonProxyStatus() == null ?
                        "UNKNOWN" : StatusReporter.getSshManagerStatus().getJsonProxyStatus())
                .add("version", getVersion() == null ?
                        "UNKNOWN" : getVersion())
                .add("isReadyToUpgrade", StatusReporter.getFieldAgentStatus().isReadyToUpgrade())
                .add("isReadyToRollback", StatusReporter.getFieldAgentStatus().isReadyToRollback())
                .build();
    }

    /**
     * executes actions after successful status post request
     */
    private void onPostStatusSuccess() {
        StatusReporter.getProcessManagerStatus().removeNotRunningMicroserviceStatus();
    }

    /**
     * checks if IOFog is not provisioned
     *
     * @return boolean
     */
    private boolean notProvisioned() {
    	logDebug("Started checking provisioned");
        boolean notProvisioned = StatusReporter.getFieldAgentStatus().getControllerStatus().equals(NOT_PROVISIONED);
        if (notProvisioned) {
            logWarning("Not provisioned");
        }
        logDebug("Finished checking provisioned : " + notProvisioned);
        return notProvisioned;
    }

    /**
     * sends IOFog instance status to IOFog controller
     */
    private void postStatusHelper() {
        logDebug("posting ioFog status");
        try {
            JsonObject status = getFogStatus();
            if (Configuration.debugging) {
                logInfo(status.toString());
            }
            connected = isControllerConnected(false);
            if (!connected)
                return;

            orchestrator.request("status", RequestType.PUT, null, status);
            onPostStatusSuccess();
        } catch (CertificateException | SSLHandshakeException | ConnectException e) {
            verificationFailed(e);
            logError("Unable to send status due to broken certificate",
                    new AgentSystemException(e.getMessage(), e));
        } catch (ForbiddenException e) {
            deProvision(true);
            logError("Unable to send status due to broken certificate",
                    new AgentSystemException(e.getMessage(), e));
        }catch (SocketTimeoutException e) {
            try {
                IOFogNetworkInterfaceManager.getInstance().updateIOFogNetworkInterface();
            } catch (SocketException | MalformedURLException ex) {
                logError("Unable to update Network interface", new AgentSystemException(ex.getMessage(), ex));
            }
        } catch (Exception e) {
            logError("Unable to send status ", new AgentSystemException(e.getMessage(), e));
        }
        logDebug("Finished posting ioFog status");
    }

    private final Runnable postStatus = () -> {
        while (true) {
            try {
                if (microserviceManager.getCurrentMicroservices().size() == StatusReporter.getProcessManagerStatus().getRunningMicroservicesCount()) {
                    Thread.sleep(Configuration.getStatusFrequency() * 1000);
                } else {
                    Thread.sleep(1 * 1000);
                    ProcessManager.getInstance().updateMicroserviceStatus();
                }
                postStatusHelper();
            } catch (Exception e) {
                logError("Unable to send status ", new AgentSystemException(e.getMessage(), e));
            }

        }
    };

    private final Runnable postDiagnostics = () -> {
        while (true) {
        	logDebug("Start posting diagnostic");
            if (StraceDiagnosticManager.getInstance().getMonitoringMicroservices().size() > 0) {
                JsonBuilderFactory factory = Json.createBuilderFactory(null);
                JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();

                for (MicroserviceStraceData microservice : StraceDiagnosticManager.getInstance().getMonitoringMicroservices()) {
                    arrayBuilder.add(factory.createObjectBuilder()
                        .add("microserviceUuid", microservice.getMicroserviceUuid())
                        .add("buffer", microservice.getResultBufferAsString())
                    );
                    microservice.getResultBuffer().clear();
                }

                JsonObject json = factory.createObjectBuilder()
                    .add("straceData", arrayBuilder).build();

                try {
                    orchestrator.request("strace", RequestType.PUT, null, json);
                } catch (Exception e) {
                	logError("Unable send strace logs", new AgentSystemException("Unable send strace logs", e));
                }
            }

            try {
                Thread.sleep(Configuration.getPostDiagnosticsFreq() * 1000);
            } catch (InterruptedException e) {
                logError("Error posting diagnostic", new AgentSystemException(e.getMessage(), e));
            }
            logDebug("Finished posting diagnostic");
        }
    };

    public final void postTracking(JsonObject events) {
    	logDebug("Start posting tracking");
        try {
            orchestrator.request("tracking", RequestType.POST, null, events);
        } catch (Exception e) {
        	logError("Unable send tracking logs", new AgentSystemException(e.getMessage(), e));
        }
        logDebug("Finished posting tracking");
    }

    /**
     * logs and sets appropriate status when controller
     * certificate is not verified
     */
    private void verificationFailed(Exception e) {
    	logDebug("Start verification Failed of controller");
        connected = false;
        if (!notProvisioned()) {
            ControllerStatus controllerStatus;
            if (e instanceof CertificateException || e instanceof SSLHandshakeException) {
                controllerStatus = BROKEN_CERTIFICATE;
            } else {
                controllerStatus = NOT_CONNECTED;
            }
            StatusReporter.setFieldAgentStatus().setControllerStatus(controllerStatus);
            logWarning("controller verification failed: " + controllerStatus.name());
        }
        StatusReporter.setFieldAgentStatus().setControllerVerified(false);
        logDebug("Finished verification Failed of Controller");
    }

    private final Future<Boolean> processChanges(JsonObject changes) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            boolean resetChanges = true;

            if (changes.getBoolean("deleteNode",false) && !initialization) {
                try {
                    deleteNode();
                } catch (Exception e) {
                    logError("Unable to delete node", e);
                    resetChanges = false;
                }
            } else {
                if (changes.getBoolean("reboot",false) && !initialization) {
                    try {
                        reboot();
                    } catch (Exception e) {
                        logError("Unable to perform reboot", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("isImageSnapshot",false) && !initialization) {
                    try {
                        createImageSnapshot();
                    } catch (Exception e) {
                        logError("Unable to create snapshot", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("config",false) && !initialization) {
                    try {
                        getFogConfig();
                    } catch (Exception e) {
                        logError("Unable to get config", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("version",false) && !initialization) {
                    try {
                        changeVersion();
                    } catch (Exception e) {
                        logError("Unable to change version", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("registries",false) || initialization) {
                    try {
                        loadRegistries(false);
                        ProcessManager.getInstance().update();
                    } catch (Exception e) {
                        logError("Unable to update registries", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("prune", false) && !initialization) {
                    try {
                        DockerPruningManager.getInstance().pruneAgent();
                    } catch (Exception e) {
                        logError("Unable to update registries", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("microserviceConfig",false) || changes.getBoolean("microserviceList",false) ||
                        changes.getBoolean("routing",false) || initialization) {
                    boolean microserviceConfig = changes.getBoolean("microserviceConfig");
                    boolean routing = changes.getBoolean("routing");
                    int defaultFreq = Configuration.getStatusFrequency();
                    Configuration.setStatusFrequency(1);
                    try {
                        List<Microservice> microservices = loadMicroservices(false);

                        if (microserviceConfig) {
                            try {
                                processMicroserviceConfig(microservices);
                                LocalApi.getInstance().update();
                            } catch (Exception e) {
                                logError("Unable to update microservices config", e);
                                resetChanges = false;
                            }
                        }

                        if (routing) {
                            try {
                                processRoutes(microservices);
                                if (!changes.getBoolean("routerChanged",false) || initialization) {
                                    MessageBus.getInstance().update();
                                }
                            } catch (Exception e) {
                                logError("Unable to update microservices routes", e);
                                resetChanges = false;
                            }
                        }
                    } catch (Exception e) {
                        logError("Unable to get microservices list", e);
                        resetChanges = false;
                    } finally {
                        Configuration.setStatusFrequency(defaultFreq);
                    }
                }

                if (changes.getBoolean("tunnel",false) && !initialization) {
                    try {
                        sshProxyManager.update(getProxyConfig());
                    } catch (Exception e) {
                        logError("Unable to create tunnel", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("diagnostics",false) && !initialization) {
                    try {
                        updateDiagnostics();
                    } catch (Exception e) {
                        logError("Unable to update diagnostics", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("routerChanged",false) && !initialization) {
                    try {
                        MessageBus.getInstance().update();
                    } catch (Exception e) {
                        logError("Unable to update router info", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("linkedEdgeResources",false) && !initialization) {
                    boolean linkedEdgeResources = changes.getBoolean("linkedEdgeResources");
                    try {
                        if (linkedEdgeResources) {
                            loadEdgeResources(false);
                            LocalApi.getInstance().updateEdgeResource();
                        }
                    } catch (Exception e) {
                        logError("Unable to update linked edge resources", e);
                        resetChanges = false;
                    }
                }
            }
            return resetChanges;
        });
    }

    /**
     * Loads edge resources from json file
     * @return
     */
    private JsonArray loadEdgeResourcesJsonFile() {
        String filename = EDGE_RESOURCE_FILE;
        JsonArray edgeResourcesJson = readFile(filesPath + filename);
        return  edgeResourcesJson;
    }

    /**
     * Loads edge resources from file or get from Controller
     * @param fromFile
     * @return
     */
    private List<EdgeResource> loadEdgeResources(boolean fromFile) {
        {
            logDebug("Start Loading edge resources...");
            List<EdgeResource> edgeResourcesList = new ArrayList<>();
            if (notProvisioned() || !isControllerConnected(fromFile)) {
                return edgeResourcesList;
            }

            String filename = EDGE_RESOURCE_FILE;
            JsonArray edgeResourcesJson = null;
            try {
                if (fromFile) {
                    edgeResourcesJson = readFile(filesPath + filename);
                    if (edgeResourcesJson == null) {
                        return loadEdgeResources(false);
                    }
                } else {
                    JsonObject result = orchestrator.request("edgeResources", RequestType.GET, null, null);
                    if(result.containsKey("edgeResources")) {
                        edgeResourcesJson = result.getJsonArray("edgeResources");
                        saveFile(edgeResourcesJson, filesPath + filename);
                    } else {
                        logError("Error loading edgeResources from IOFog controller",
                                new AgentUserException("Error loading microservices from IOFog controller"));
                    }
                }
                try {
                    if (edgeResourcesJson != null){
                        List<EdgeResource> edgeResources = IntStream.range(0, edgeResourcesJson.size())
                                .boxed()
                                .map(edgeResourcesJson::getJsonObject)
                                .map(containerJsonObjectToEdgeResourcesFunction())
                                .collect(toList());
                        edgeResourceManager.setLatestEdgeResources(edgeResources);
                        edgeResourcesList.addAll(edgeResources);
                        logDebug("Edge resource list size : " + edgeResourcesList.size());
                    }
                } catch (Exception e) {
                    logError("Unable to parse edgeResources", new AgentSystemException(e.getMessage(), e));
                }
            } catch (CertificateException | SSLHandshakeException e) {
                verificationFailed(e);
                logError("Unable to get edgeResources due to broken certificate",
                        new AgentSystemException(e.getMessage(), e));
            } catch (Exception e) {
                logError("Unable to get edgeResources", new AgentSystemException(e.getMessage(), e));
            }
            logDebug("Finished loading edge resources...");
            return edgeResourcesList;
        }
    }

    /**
     * maps json object to EdgeResources
     * @return
     */
    private Function<JsonObject, EdgeResource> containerJsonObjectToEdgeResourcesFunction() {
        return jsonObj -> {
            EdgeResource edgeResource = new EdgeResource(jsonObj.getInt("id"), jsonObj.getString("name"), jsonObj.getString("version"));
            JsonObject customData = jsonObj.getJsonObject("custom");
            try {
                if (customData != null && !customData.getValueType().equals(JsonValue.ValueType.NULL) && customData.size() > 0) {
                    HashMap<String,Object> customMap = new ObjectMapper().readValue(
                            customData.toString(),
                            new TypeReference<HashMap<String,Object>>() {
                            });
                    edgeResource.setCustom(customMap);
                }
            } catch (Exception e) {
                logError("Error mapping custom field of edgeResources", new AgentSystemException(e.getMessage(), e));
            }
            Display display = new Display();
            EdgeInterface edgeInterface = new EdgeInterface();
            edgeResource.setDescription(jsonObj.getString("description", null));
            edgeResource.setInterfaceProtocol(jsonObj.getString("interfaceProtocol", null));
            JsonArray tags = jsonObj.getJsonArray("orchestrationTags");
            String[] orchestrationTags = null;
            if (tags != null && !tags.getValueType().equals(JsonValue.ValueType.NULL) && tags.size() > 0) {
                List<String> result = IntStream.range(0, tags.size())
                        .boxed()
                        .map(tags::getString)
                        .collect(Collectors.toList());
                orchestrationTags = result.toArray(new String[result.size()]);
            }

            edgeResource.setOrchestrationTags(orchestrationTags);
            JsonObject displayValue = jsonObj.getJsonObject("display");
            if (displayValue != null && !displayValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                display.setName(displayValue.getString("name", null));
                display.setIcon(displayValue.getString("icon", null));
                display.setColor(displayValue.getString("color", null));
            }

            edgeResource.setDisplay(display);
            JsonObject interfaceObj = jsonObj.getJsonObject("interface");
            if (interfaceObj != null && !interfaceObj.getValueType().equals(JsonValue.ValueType.NULL)) {
                edgeInterface.setEdgeResourceId(interfaceObj.getInt("edgeResourceId"));
                edgeInterface.setId(interfaceObj.getInt("id"));
                JsonArray endpointsArray = interfaceObj.getJsonArray("endpoints");
                if (endpointsArray != null && !endpointsArray.getValueType().equals(JsonValue.ValueType.NULL)) {
                    List<EdgeEndpoints> edgeEndpoints = endpointsArray.size() > 0
                            ? IntStream.range(0, endpointsArray.size())
                            .boxed()
                            .map(endpointsArray::getJsonObject)
                            .map(endpoint -> {
                                EdgeEndpoints edgeEndPoint = new EdgeEndpoints();
                                edgeEndPoint.setDescription(endpoint.getString("description", null));
                                edgeEndPoint.setId(endpoint.getInt("id"));
                                edgeEndPoint.setInterfaceId(endpoint.getInt("interfaceId"));
                                edgeEndPoint.setMethod(endpoint.getString("method", null));
                                edgeEndPoint.setName(endpoint.getString("name", null));
                                edgeEndPoint.setRequestPayloadExample(endpoint.getString("requestPayloadExample", null));
                                edgeEndPoint.setRequestType(endpoint.getString("requestType", null));
                                edgeEndPoint.setResponseType(endpoint.getString("responseType", null));
                                edgeEndPoint.setResponsePayloadExample(endpoint.getString("responsePayloadExample", null));
                                edgeEndPoint.setUrl(endpoint.getString("url", null));
                                return edgeEndPoint;
                            })
                            .collect(toList())
                            : null;
                    edgeInterface.setEndpoints(edgeEndpoints);
                }
            }
            edgeResource.setEdgeInterface(edgeInterface);
            return edgeResource;
        };
    }

    /**
     * retrieves IOFog changes list from IOFog controller
     */
    private final Runnable getChangesList = () -> {
        while (true) {
            try {
                int frequency = Configuration.getChangeFrequency() * 1000;
                Thread.sleep(frequency);
                logDebug("Start get IOFog changes list from IOFog controller");

                if (notProvisioned() || !isControllerConnected(false)) {
                    logDebug("Cannot get change list due to controller status not provisioned or controller not connected");
                    continue;
                }


                JsonObject result;
                try {
                    result = orchestrator.request("config/changes", RequestType.GET, null, null);
                } catch (CertificateException | SSLHandshakeException e) {
                    verificationFailed(e);
                    logError("Unable to get changes due to broken certificate",
                    		new AgentSystemException(e.getMessage(), e));
                    continue;
                } catch (SocketTimeoutException e) {
                    IOFogNetworkInterfaceManager.getInstance().updateIOFogNetworkInterface();
                    continue;
                } catch (Exception e) {
                    logError("Unable to get changes ", new AgentSystemException(e.getMessage(), e));
                    continue;
                }


                StatusReporter.setFieldAgentStatus().setLastCommandTime(lastGetChangesList);

                String lastUpdated = result.getString("lastUpdated", null);
                boolean resetChanges;
                Future<Boolean> changesProcessor = processChanges(result);

                try {
                    resetChanges = changesProcessor.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    resetChanges = false;
                    changesProcessor.cancel(true);
                }

                if (lastUpdated != null && resetChanges) {
                    logDebug("Resetting config changes flags");
                    try {
                        JsonObject req = Json.createObjectBuilder()
                                .add("lastUpdated", lastUpdated)
                                .build();
                        orchestrator.request("config/changes", RequestType.PATCH, null, req);
                    } catch (Exception e) {
                        logError("Resetting config changes has failed", e);
                    }
                }

                initialization = initialization && !resetChanges;
            } catch (Exception e) {
            	logError("Error getting changes list ", new AgentSystemException(e.getMessage(), e));
            }
            logDebug("Finish get IOFog changes list from IOFog controller");
        }
    };

    /**
     * Deletes current fog node from controller and makes deprovision
     */
    private void deleteNode() {
        logDebug("start deleting current fog node from controller and make it deprovision");
        try {
            orchestrator.request("delete-node", RequestType.DELETE, null, null);
        } catch (Exception e) {
            logError("Can't send delete node command",
            		new AgentSystemException("Can't send delete node command", e));
        }
        deProvision(false);
        logDebug("Finish deleting current fog node from controller and make it deprovision");
    }

    /**
     * Remote reboot of Linux machine from IOFog controller
     */
    private void reboot() {
    	logInfo("start Remote reboot of Linux machine from IOFog controller");
        LoggingService.logInfo(MODULE_NAME, "Rebooting");
        if (SystemUtils.IS_OS_WINDOWS) {
            return; // TODO implement
        }

        CommandShellResultSet<List<String>, List<String>> result = CommandShellExecutor.executeCommand("shutdown -r now");
        if (result == null) {
            LoggingService.logError(MODULE_NAME, "Error in Remote reboot of Linux machine from IOFog controller",
                    new AgentSystemException("Error in Remote reboot of Linux machine from IOFog controller"));
        }
        if (result != null && result.getError().size() > 0) {
            LoggingService.logWarning(MODULE_NAME, result.toString());
        }
        logInfo("Finished Remote reboot of Linux machine from IOFog controller");
    }

    /**
     * performs change version operation, received from ioFog controller
     */
    private void changeVersion() {
        LoggingService.logInfo(MODULE_NAME, "Starting change version action");

        if (notProvisioned() || !isControllerConnected(false)) {
            return;
        }

        try {
            JsonObject result = orchestrator.request("version", RequestType.GET, null, null);
            VersionHandler.changeVersion(result);

        } catch (CertificateException | SSLHandshakeException e) {
            verificationFailed(e);
            LoggingService.logError(MODULE_NAME, "Unable to get version command due to broken certificate",
            		new AgentSystemException(e.getMessage(), e));
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Unable to get version command",
            		new AgentSystemException(e.getMessage(), e));
        }
        LoggingService.logInfo(MODULE_NAME, "Finished change version operation, received from ioFog controller");
    }

    private void updateDiagnostics() {
        LoggingService.logInfo(MODULE_NAME, "Start update diagnostics");
        if (notProvisioned() || !isControllerConnected(false)) {
            return;
        }

        if (SystemUtils.IS_OS_WINDOWS) {
            return; // TODO implement
        }

        try {
            JsonObject result = orchestrator.request("strace", RequestType.GET, null, null);

            StraceDiagnosticManager.getInstance().updateMonitoringMicroservices(result);

        } catch (CertificateException | SSLHandshakeException e) {
            verificationFailed(e);
            LoggingService.logError(MODULE_NAME, "Unable to get diagnostics update due to broken certificate",
            		new AgentSystemException(e.getMessage(), e));
        } catch (Exception e) {
        	LoggingService.logError(MODULE_NAME, "Unable to get diagnostics update",
            		new AgentSystemException(e.getMessage(), e));
        }
        LoggingService.logInfo(MODULE_NAME, "Finished update diagnostics");
    }

    /**
     * gets list of registries from file or IOFog controller
     *
     * @param fromFile - load from file
     */
    private void loadRegistries(boolean fromFile) {
        logDebug("get registries");
        if (notProvisioned() || !isControllerConnected(fromFile)) {
            return;
        }

        String filename = "registries.json";
        try {
            JsonArray registriesList = null;
            if (fromFile) {
                registriesList = readFile(filesPath + filename);
                if (registriesList == null) {
                    loadRegistries(false);
                    return;
                }
            } else {
                JsonObject result = orchestrator.request("registries", RequestType.GET, null, null);
                if(result.containsKey("registries")) {
                    registriesList = result.getJsonArray("registries");
                    saveFile(registriesList, filesPath + filename);
                } else {
                    logError("Error loading registries from IOFog controller",
                            new AgentUserException("Error loading registries from IOFog controller"));
                }
            }
            List<Registry> registries = new ArrayList<>();
            if (registriesList != null && registriesList.size() != 0) {
                for (int i = 0; i < registriesList.size(); i++) {
                    JsonObject registry = registriesList.getJsonObject(i);
                    Registry.RegistryBuilder registryBuilder = new Registry.RegistryBuilder()
                            .setId(registry.getInt("id"))
                            .setUrl(registry.getString("url"))
                            .setIsPublic(registry.getBoolean("isPublic", false));
                    if (!registry.getBoolean("isPublic", false)) {
                        registryBuilder.setUserName(registry.getString("username"))
                                .setPassword(registry.getString("password"))
                                .setUserEmail(registry.getString("userEmail"));

                    }
                    registries.add(registryBuilder.build());
                }
                microserviceManager.setRegistries(registries);
            } else {
                logInfo("Registries list is empty");
            }
        } catch (CertificateException | SSLHandshakeException e) {
            verificationFailed(e);
            logError("Unable to get registries due to broken certificate",
            		new AgentUserException(e.getMessage(), e));
        }  catch (AgentUserException e) {
            logError("Unable to get registries",
            		new AgentUserException(e.getMessage(), e));
        } catch (AgentSystemException e) {
            logError("Unable to get registries",
            		new AgentUserException(e.getMessage(), e));
        } catch (Exception e) {
            logError("Unable to get registries", new AgentSystemException(e.getMessage(), e));
        }
        logDebug("Finished get registries");
    }

    /**
     * gets list of Microservice configurations from file or IOFog controller
     */
    private void processMicroserviceConfig(List<Microservice> microservices) {
    	logDebug("Start process microservice configuration");
        Map<String, String> configs = new HashMap<>();
        for (Microservice microservice : microservices) {
            configs.put(microservice.getMicroserviceUuid(), microservice.getConfig());
        }

        microserviceManager.setConfigs(configs);
        logDebug("Finished process microservice configuration");
    }

    /**
     * gets list of Microservice routings from file or IOFog controller
     */
    private void processRoutes(List<Microservice> microservices) {
        Map<String, Route> routes = new HashMap<>();
        for (Microservice microservice : microservices) {
            List<String> jsonRoutes = microservice.getRoutes();
            if (jsonRoutes == null || jsonRoutes.size() == 0) {
                continue;
            }

            String microserviceUuid = microservice.getMicroserviceUuid();
            Route microserviceRoute = new Route();

            for (String jsonRoute : jsonRoutes) {
                microserviceRoute.getReceivers().add(jsonRoute);
            }

            routes.put(microserviceUuid, microserviceRoute);
        }

        microserviceManager.setRoutes(routes);
        logDebug("Finished process routes");
    }

    private JsonArray loadMicroservicesJsonFile() {
        String filename = MICROSERVICE_FILE;
        JsonArray microservicesJson = readFile(filesPath + filename);
        return  microservicesJson;
    }

    /**
     * gets list of Microservices from file or IOFog controller
     *
     * @param fromFile - load from file
     */
    private List<Microservice> loadMicroservices(boolean fromFile) {
        logDebug("Start Loading microservices...");
        List<Microservice> microserviceList = new ArrayList<>();
        if (notProvisioned() || !isControllerConnected(fromFile)) {
            return microserviceList;
        }

        String filename = MICROSERVICE_FILE;
        JsonArray microservicesJson = null;
        try {
            if (fromFile) {
                microservicesJson = readFile(filesPath + filename);
                if (microservicesJson == null) {
                    return loadMicroservices(false);
                }
            } else {
                JsonObject result = orchestrator.request("microservices", RequestType.GET, null, null);
                if(result.containsKey("microservices")) {
                    microservicesJson = result.getJsonArray("microservices");
                    saveFile(microservicesJson, filesPath + filename);
                } else {
                    logError("Error loading microservices from IOFog controller",
                            new AgentUserException("Error loading microservices from IOFog controller"));
                }
            }
            try {
                if (microservicesJson != null){
                    List<Microservice> microservices = IntStream.range(0, microservicesJson.size())
                            .boxed()
                            .map(microservicesJson::getJsonObject)
                            .map(containerJsonObjectToMicroserviceFunction())
                            .collect(toList());
                    microserviceManager.setLatestMicroservices(microservices);
                    microserviceList.addAll(microservices);
                }
            } catch (Exception e) {
                logError("Unable to parse microservices", new AgentSystemException(e.getMessage(), e));
            }
        } catch (CertificateException | SSLHandshakeException e) {
            verificationFailed(e);
            logError("Unable to get microservices due to broken certificate",
            		new AgentSystemException(e.getMessage(), e));
        } catch (Exception e) {
            logError("Unable to get microservices", new AgentSystemException(e.getMessage(), e));
        }
        logDebug("Finished Loading microservices...");
        return microserviceList;
    }

    private List<String> getStringList(JsonValue jsonValue) {
        if (jsonValue != null && !jsonValue.getValueType().equals(JsonValue.ValueType.NULL)) {
            JsonArray valueObj = (JsonArray) jsonValue;
            return valueObj.size() > 0
                    ? IntStream.range(0, valueObj.size())
                    .boxed()
                    .map(valueObj::getString)
                    .collect(toList())
                    : null;
        }

        return null;
    }

    private Function<JsonObject, Microservice> containerJsonObjectToMicroserviceFunction() {
        return jsonObj -> {
            Microservice microservice = new Microservice(jsonObj.getString("uuid"), jsonObj.getString("imageId"));
            microservice.setConfig(jsonObj.getString("config"));
            microservice.setRebuild(jsonObj.getBoolean("rebuild"));
            microservice.setRootHostAccess(jsonObj.getBoolean("rootHostAccess"));
            microservice.setRegistryId(jsonObj.getInt("registryId"));
            microservice.setLogSize(jsonObj.getJsonNumber("logSize").longValue());
            microservice.setDelete(jsonObj.getBoolean("delete"));
            microservice.setDeleteWithCleanup(jsonObj.getBoolean("deleteWithCleanup"));

            JsonValue routesValue = jsonObj.get("routes");
            microservice.setRoutes(getStringList(routesValue));

            microservice.setConsumer(jsonObj.getBoolean("isConsumer"));

            JsonValue portMappingValue = jsonObj.get("portMappings");
            if (!portMappingValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                JsonArray portMappingObjs = (JsonArray) portMappingValue;
                List<PortMapping> pms = portMappingObjs.size() > 0
                        ? IntStream.range(0, portMappingObjs.size())
                        .boxed()
                        .map(portMappingObjs::getJsonObject)
                        .map(portMapping -> new PortMapping(portMapping.getInt("portExternal"),
                                portMapping.getInt("portInternal"),
                                portMapping.getBoolean("isUdp", false)))
                        .collect(toList())
                        : null;

                microservice.setPortMappings(pms);
            }

            JsonValue volumeMappingValue = jsonObj.get("volumeMappings");
            if (!volumeMappingValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                JsonArray volumeMappingObj = (JsonArray) volumeMappingValue;
                List<VolumeMapping> vms = volumeMappingObj.size() > 0
                        ? IntStream.range(0, volumeMappingObj.size())
                        .boxed()
                        .map(volumeMappingObj::getJsonObject)
                        .map(volumeMapping -> {
                            VolumeMappingType volumeMappingType = volumeMapping.getString("type", "bind").equals("volume") ? VolumeMappingType.VOLUME : VolumeMappingType.BIND;
                            return new VolumeMapping(volumeMapping.getString("hostDestination"),
                                    volumeMapping.getString("containerDestination"),
                                    volumeMapping.getString("accessMode"),
                                    volumeMappingType);
                        }).collect(toList()) : null;

                microservice.setVolumeMappings(vms);
            }

            JsonValue envVarsValue = jsonObj.get("env");
            if (envVarsValue != null && !envVarsValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                JsonArray envVarsObjs = (JsonArray) envVarsValue;
                List<EnvVar> envs = envVarsObjs.size() > 0
                        ? IntStream.range(0, envVarsObjs.size())
                        .boxed()
                        .map(envVarsObjs::getJsonObject)
                        .map(env -> new EnvVar(env.getString("key"),
                                env.getString("value")))
                        .collect(toList())
                        : null;

                microservice.setEnvVars(envs);
            }

            JsonValue argsValue = jsonObj.get("cmd");
            microservice.setArgs(getStringList(argsValue));

            JsonValue extraHostsValue = jsonObj.get("extraHosts");
            microservice.setExtraHosts(getStringList(extraHostsValue));

            try {
                LoggingService.setupMicroserviceLogger(microservice.getMicroserviceUuid(), microservice.getLogSize());
            } catch (IOException e) {
                logError("Error at setting up microservice logger",
                		new AgentSystemException(e.getMessage(), e));
            }
            return microservice;
        };
    }

    /**
     * pings IOFog controller
     */
    private boolean ping() {
    	logDebug("Started Ping");
        if (notProvisioned()) {
        	logInfo("Finished Ping : " + false);
            return false;
        }

        try {
            if (orchestrator.ping()) {
                StatusReporter.setFieldAgentStatus().setControllerStatus(OK);
                StatusReporter.setFieldAgentStatus().setControllerVerified(true);
                logDebug("Finished Ping : " + true);
                return true;
            }
        } catch (CertificateException | SSLHandshakeException e) {
            verificationFailed(e);
            logError("Error pinging controller due to broken certificate",
            		new AgentSystemException(e.getMessage(), e));
        } catch (Exception e) {
            verificationFailed(e);
            logError("Error pinging controller", new AgentUserException(e.getMessage(), e));
        }
        logDebug("Finished Ping : " + false);
        return false;
    }

    /**
     * pings IOFog controller
     */
    private final Runnable pingController = () -> {
        while (true) {
            try {
                Thread.sleep(Configuration.getPingControllerFreqSeconds() * 1000);
                logDebug("Start Ping controller");
                ping();
            } catch (Exception e) {
                logError("Exception pinging controller", new AgentUserException(e.getMessage(), e));
            }
            logDebug("Finished Ping controller");
        }
    };

    /**
     * computes SHA1 checksum
     *
     * @param data - input data
     * @return String
     */
    private String checksum(String data) {
        try {
            byte[] base64 = Base64.getEncoder().encode(data.getBytes(UTF_8));
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(base64);
            byte[] mdbytes = md.digest();
            StringBuilder sb = new StringBuilder("");
            for (byte mdbyte : mdbytes) {
                sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (Exception e) {
            logError("Error computing checksum", new AgentSystemException(e.getMessage(), e));
            return "";
        }
    }

    /**
     * reads json data from file and compare data checksum
     * if checksum failed, returns null
     *
     * @param filename - file name to read data from
     * @return JsonArray
     */
    private JsonArray readFile(String filename) {
    	logDebug(String.format("Start read file %s :", filename));
        if (!Files.exists(Paths.get(filename), NOFOLLOW_LINKS))
            return null;

        JsonObject object = readObject(filename);
        String checksum = object.getString("checksum");
        JsonArray data = object.getJsonArray("data");
        if (!checksum(data.toString()).equals(checksum))
            return null;
        long timestamp = object.getJsonNumber("timestamp").longValue();
        if (lastGetChangesList == 0)
            lastGetChangesList = timestamp;
        else
            lastGetChangesList = Long.min(timestamp, lastGetChangesList);
        logDebug("Finished read file");
        return data;
    }

    private JsonObject readObject(String filename) {
        JsonObject object = null;
        try (JsonReader reader = Json.createReader(new InputStreamReader(new FileInputStream(filename), UTF_8))) {
            object = reader.readObject();
        } catch (FileNotFoundException ex) {
            LoggingService.logError(MODULE_NAME, "Invalid file: " + filename, new AgentUserException(ex.getMessage(), ex));
        }
        return object;
    }

    /**
     * saves data and checksum to json file
     *
     * @param data     - data to be written into file
     * @param filename - file name
     */
    private void saveFile(JsonArray data, String filename) {
    	logDebug("Start save file name : " + filename);
        String checksum = checksum(data.toString());
        JsonObject object = Json.createObjectBuilder()
                .add("checksum", checksum)
                .add("timestamp", lastGetChangesList)
                .add("data", data)
                .build();
        try (JsonWriter writer = Json.createWriter(new OutputStreamWriter(new FileOutputStream(filename), UTF_8))) {
             writer.writeObject(object);
        } catch (IOException e) {
            logError("Error saving data to file '" + filename + "'",
                    new AgentUserException(e.getMessage(), e));
        }
        logDebug("Finished save file");
    }

    /**
     * gets IOFog instance configuration from IOFog controller
     */
    private void getFogConfig() {
        logInfo("Starting Get ioFog config");
        boolean hasError = false;
        if (notProvisioned() || !isControllerConnected(false)) {
            return;
        }

        if (initialization) {
            postFogConfig();
            return;
        }

        try {
            JsonObject configs = orchestrator.request("config", RequestType.GET, null, null);
            if (configs != null && configs.size() != 0) {
                String networkInterface = configs.containsKey(NETWORK_INTERFACE.getJsonProperty())  ?
                        configs.getString(NETWORK_INTERFACE.getJsonProperty()) :
                        NETWORK_INTERFACE.getDefaultValue();
                String dockerUrl = configs.containsKey(DOCKER_URL.getJsonProperty())  ?
                        configs.getString(DOCKER_URL.getJsonProperty()) :
                        DOCKER_URL.getDefaultValue();
                double diskLimit = configs.containsKey(DISK_CONSUMPTION_LIMIT.getJsonProperty()) ?
                        configs.getJsonNumber(DISK_CONSUMPTION_LIMIT.getJsonProperty()).doubleValue() :
                        Double.parseDouble(DISK_CONSUMPTION_LIMIT.getDefaultValue());
                String diskDirectory = configs.containsKey(DISK_DIRECTORY.getJsonProperty()) ?
                        configs.getString(DISK_DIRECTORY.getJsonProperty()) :
                        DISK_DIRECTORY.getDefaultValue();
                double memoryLimit = configs.containsKey(MEMORY_CONSUMPTION_LIMIT.getJsonProperty()) ?
                        configs.getJsonNumber(MEMORY_CONSUMPTION_LIMIT.getJsonProperty()).doubleValue() :
                        Double.parseDouble(MEMORY_CONSUMPTION_LIMIT.getDefaultValue());
                double cpuLimit = configs.containsKey(PROCESSOR_CONSUMPTION_LIMIT.getJsonProperty()) ?
                        configs.getJsonNumber(PROCESSOR_CONSUMPTION_LIMIT.getJsonProperty()).doubleValue() :
                        Double.parseDouble(PROCESSOR_CONSUMPTION_LIMIT.getDefaultValue());
                double logLimit = configs.containsKey(LOG_DISK_CONSUMPTION_LIMIT.getJsonProperty()) ?
                        configs.getJsonNumber(LOG_DISK_CONSUMPTION_LIMIT.getJsonProperty()).doubleValue() :
                        Double.parseDouble(LOG_DISK_CONSUMPTION_LIMIT.getDefaultValue());
                String logDirectory = configs.containsKey(LOG_DISK_DIRECTORY.getJsonProperty()) ?
                        configs.getString(LOG_DISK_DIRECTORY.getJsonProperty()) :
                        LOG_DISK_DIRECTORY.getDefaultValue();
                int logFileCount = configs.containsKey(LOG_FILE_COUNT.getJsonProperty()) ?
                        configs.getInt(LOG_FILE_COUNT.getJsonProperty()) :
                        Integer.parseInt(LOG_FILE_COUNT.getDefaultValue());
                int statusFrequency = configs.containsKey(STATUS_FREQUENCY.getJsonProperty()) ?
                        configs.getInt(STATUS_FREQUENCY.getJsonProperty()) :
                        Integer.parseInt(STATUS_FREQUENCY.getDefaultValue());
                int changeFrequency = configs.containsKey(CHANGE_FREQUENCY.getJsonProperty()) ?
                        configs.getInt(CHANGE_FREQUENCY.getJsonProperty()) :
                        Integer.parseInt(CHANGE_FREQUENCY.getDefaultValue());
                int deviceScanFrequency = configs.containsKey(DEVICE_SCAN_FREQUENCY.getJsonProperty()) ?
                        configs.getInt(DEVICE_SCAN_FREQUENCY.getJsonProperty()) :
                        Integer.parseInt(DEVICE_SCAN_FREQUENCY.getDefaultValue());
                boolean watchdogEnabled = configs.containsKey(WATCHDOG_ENABLED.getJsonProperty()) ?
                        configs.getBoolean(WATCHDOG_ENABLED.getJsonProperty()) :
                        WATCHDOG_ENABLED.getDefaultValue().equalsIgnoreCase("OFF") ? false : true;
                double latitude = configs.containsKey("latitude") ?
                        configs.getJsonNumber("latitude").doubleValue() :
                        0;
                double longitude = configs.containsKey("longitude") ?
                        configs.getJsonNumber("longitude").doubleValue() :
                        0;
                String gpsCoordinates = latitude + "," + longitude;
                String logLevel = configs.containsKey(LOG_LEVEL.getJsonProperty()) ?
                        configs.getString(LOG_LEVEL.getJsonProperty()) :
                        LOG_LEVEL.getDefaultValue();

                int dockerPruningFrequency = configs.containsKey(DOCKER_PRUNING_FREQUENCY.getJsonProperty()) ?
                        configs.getInt(DOCKER_PRUNING_FREQUENCY.getJsonProperty()) :
                        Integer.parseInt(DOCKER_PRUNING_FREQUENCY.getDefaultValue());

                int availableDiskThreshold = configs.containsKey(AVAILABLE_DISK_THRESHOLD.getJsonProperty()) ?
                        configs.getInt(AVAILABLE_DISK_THRESHOLD.getJsonProperty()) :
                        Integer.parseInt(AVAILABLE_DISK_THRESHOLD.getDefaultValue());

                int readyToUpgradeScanFreq = configs.containsKey(READY_TO_UPGRADE_SCAN_FREQUENCY.getJsonProperty()) ?
                        configs.getInt(READY_TO_UPGRADE_SCAN_FREQUENCY.getJsonProperty()) :
                        Integer.parseInt(READY_TO_UPGRADE_SCAN_FREQUENCY.getDefaultValue());

                String timeZone = configs.containsKey(TIME_ZONE.getJsonProperty()) ?
                        configs.getString(TIME_ZONE.getJsonProperty()) :
                        TIME_ZONE.getDefaultValue();

                Map<String, Object> instanceConfig = new HashMap<>();

                if (!NETWORK_INTERFACE.getDefaultValue().equals(Configuration.getNetworkInterface()) &&
                        !Configuration.getNetworkInterface().equals(networkInterface))
                    instanceConfig.put(NETWORK_INTERFACE.getCommandName(), networkInterface);

                if (Configuration.getDockerUrl() != null && !Configuration.getDockerUrl().equals(dockerUrl))
                    instanceConfig.put(DOCKER_URL.getCommandName(), dockerUrl);

                if (Configuration.getDiskLimit() != diskLimit)
                    instanceConfig.put(DISK_CONSUMPTION_LIMIT.getCommandName(), diskLimit);

                if (Configuration.getDiskDirectory() != null && !Configuration.getDiskDirectory().equals(diskDirectory))
                    instanceConfig.put(DISK_DIRECTORY.getCommandName(), diskDirectory);

                if (Configuration.getMemoryLimit() != memoryLimit)
                    instanceConfig.put(MEMORY_CONSUMPTION_LIMIT.getCommandName(), memoryLimit);

                if (Configuration.getCpuLimit() != cpuLimit)
                    instanceConfig.put(PROCESSOR_CONSUMPTION_LIMIT.getCommandName(), cpuLimit);

                if (Configuration.getLogDiskLimit() != logLimit)
                    instanceConfig.put(LOG_DISK_CONSUMPTION_LIMIT.getCommandName(), logLimit);

                if (Configuration.getLogDiskDirectory() != null && !Configuration.getLogDiskDirectory().equals(logDirectory))
                    instanceConfig.put(LOG_DISK_DIRECTORY.getCommandName(), logDirectory);

                if (Configuration.getLogFileCount() != logFileCount)
                    instanceConfig.put(LOG_FILE_COUNT.getCommandName(), logFileCount);

                if (Configuration.getStatusFrequency() != statusFrequency)
                    instanceConfig.put(STATUS_FREQUENCY.getCommandName(), statusFrequency);

                if (Configuration.getChangeFrequency() != changeFrequency)
                    instanceConfig.put(CHANGE_FREQUENCY.getCommandName(), changeFrequency);

                if (Configuration.getDeviceScanFrequency() != deviceScanFrequency)
                    instanceConfig.put(DEVICE_SCAN_FREQUENCY.getCommandName(), deviceScanFrequency);

                if (Configuration.isWatchdogEnabled() != watchdogEnabled)
                    instanceConfig.put(WATCHDOG_ENABLED.getCommandName(), watchdogEnabled ? "on" : "off");

                if (Configuration.getGpsCoordinates() != null && !Configuration.getGpsCoordinates().equals(gpsCoordinates)) {
                    instanceConfig.put(GPS_MODE.getCommandName(), gpsCoordinates);
                }

                if (Configuration.getGpsCoordinates() == null && !gpsCoordinates.equals("0.0,0.0")) {
                    instanceConfig.put(GPS_MODE.getCommandName(), gpsCoordinates);
                }

                if (Configuration.getLogLevel() != null && !Configuration.getLogLevel().equals(logLevel))
                    instanceConfig.put(LOG_LEVEL.getCommandName(), logLevel);

                if ((Configuration.getDockerPruningFrequency() != dockerPruningFrequency) && (dockerPruningFrequency >= 1))
                    instanceConfig.put(DOCKER_PRUNING_FREQUENCY.getCommandName(), dockerPruningFrequency);

                if (Configuration.getAvailableDiskThreshold() != availableDiskThreshold  && (availableDiskThreshold >= 1)) {
                    instanceConfig.put(AVAILABLE_DISK_THRESHOLD.getCommandName(), availableDiskThreshold);
                }
                if ((Configuration.getReadyToUpgradeScanFrequency() != readyToUpgradeScanFreq) && (readyToUpgradeScanFreq >= 1))
                    instanceConfig.put(READY_TO_UPGRADE_SCAN_FREQUENCY.getCommandName(), readyToUpgradeScanFreq);

                if (Configuration.getTimeZone()!= null && !Configuration.getTimeZone().equals(timeZone))
                    instanceConfig.put(TIME_ZONE.getCommandName(), timeZone);

                if (!instanceConfig.isEmpty())
                    Configuration.setConfig(instanceConfig, false);
            }
        } catch (CertificateException | SSLHandshakeException e) {
            hasError = true;
            verificationFailed(e);
            logError("Unable to get ioFog config due to broken certificate",
            		new AgentUserException(e.getMessage(), e));
        } catch (Exception e) {
            hasError = true;
            try {
                logError("Unable to get ioFog config ", new AgentUserException("Unable to get ioFog config", e));
            } catch (Exception ex){
                logError("We should never see this", new AgentUserException("This exception arise while logging the exception"));
            }
        } finally {
            if (!hasError) {
                Configuration.updateConfigBackUpFile();
            }
        }
        logInfo("Finished Get ioFog config");
    }

    /**
     * sends IOFog instance configuration to IOFog controller
     */
    private void postFogConfig() {
        logInfo("Post ioFog config");
        if (notProvisioned() || !isControllerConnected(false)) {
            return;
        }

        double latitude = 0, longitude = 0;
        try {
            String gpsCoordinates = Configuration.getGpsCoordinates();
            if (gpsCoordinates != null) {
                String[] coords = gpsCoordinates.split(",");
                latitude = Double.parseDouble(coords[0]);
                longitude = Double.parseDouble(coords[1]);
            }
        } catch (Exception e) {
            logError("Error while parsing GPS coordinates", new AgentSystemException(e.getMessage(), e));
        }

        Pair<NetworkInterface, InetAddress> connectedAddress = IOFogNetworkInterfaceManager.getInstance().getNetworkInterface();
        JsonObject json = Json.createObjectBuilder()
                .add(NETWORK_INTERFACE.getJsonProperty(), connectedAddress == null ? "UNKNOWN" : connectedAddress._1().getName())
                .add(DOCKER_URL.getJsonProperty(), Configuration.getDockerUrl() == null ? "UNKNOWN" : Configuration.getDockerUrl())
                .add(DISK_CONSUMPTION_LIMIT.getJsonProperty(), Configuration.getDiskLimit())
                .add(DISK_DIRECTORY.getJsonProperty(), Configuration.getDiskDirectory() == null ? "UNKNOWN" : Configuration.getDiskDirectory())
                .add(MEMORY_CONSUMPTION_LIMIT.getJsonProperty(), Configuration.getMemoryLimit())
                .add(PROCESSOR_CONSUMPTION_LIMIT.getJsonProperty(), Configuration.getCpuLimit())
                .add(LOG_DISK_CONSUMPTION_LIMIT.getJsonProperty(), Configuration.getLogDiskLimit())
                .add(LOG_DISK_DIRECTORY.getJsonProperty(), Configuration.getLogDiskDirectory() == null ? "UNKNOWN" : Configuration.getLogDiskDirectory())
                .add(LOG_FILE_COUNT.getJsonProperty(), Configuration.getLogFileCount())
                .add(STATUS_FREQUENCY.getJsonProperty(), Configuration.getStatusFrequency())
                .add(CHANGE_FREQUENCY.getJsonProperty(), Configuration.getChangeFrequency())
                .add(DEVICE_SCAN_FREQUENCY.getJsonProperty(), Configuration.getDeviceScanFrequency())
                .add(WATCHDOG_ENABLED.getJsonProperty(), Configuration.isWatchdogEnabled())
                .add(GPS_MODE.getJsonProperty(), Configuration.getGpsMode() == null ? "UNKNOWN" : Configuration.getGpsMode().name().toLowerCase())
                .add("latitude", latitude)
                .add("longitude", longitude)
                .add(LOG_LEVEL.getJsonProperty(), Configuration.getLogLevel().toUpperCase())
                .add(AVAILABLE_DISK_THRESHOLD.getJsonProperty(), Configuration.getAvailableDiskThreshold())
                .add(DOCKER_PRUNING_FREQUENCY.getJsonProperty(), Configuration.getDockerPruningFrequency())
                .add(READY_TO_UPGRADE_SCAN_FREQUENCY.getJsonProperty(), Configuration.getReadyToUpgradeScanFrequency())
                .build();

        try {
            orchestrator.request("config", RequestType.PATCH, null, json);
        } catch (CertificateException | SSLHandshakeException e) {
            verificationFailed(e);
            logError("Unable to post ioFog config due to broken certificate ",
            		new AgentSystemException(e.getMessage(), e));
        } catch (Exception e) {
            logError("Unable to post ioFog config ", new AgentSystemException(e.getMessage(), e));
        }
        logInfo("Finished Post ioFog config");
    }

    /**
     * gets IOFog proxy configuration from IOFog controller
     */
    private JsonObject getProxyConfig() {
        JsonObject result = null;

        if (!notProvisioned() && isControllerConnected(false)) {
            try {
                JsonObject response = orchestrator.request("tunnel", RequestType.GET, null, null);
                result = response.getJsonObject("tunnel");
            } catch (Exception e) {
            	logError("Unable to get proxy config ", new AgentSystemException(e.getMessage(), e));
            }
        }
        return result;
    }

    /**
     * does the provisioning.
     * If successfully provisioned, updates Iofog UUID and Access Token in
     * configuration file and loads Microservice data, otherwise sets appropriate
     * status.
     *
     * @param key - provisioning key sent by command-line
     * @return String
     */
    public JsonObject provision(String key) {
        logInfo("Provisioning ioFog agent");
        JsonObject provisioningResult;

        if (!notProvisioned()) {
            try {
                logInfo("Agent is already provisioned. Deprovisioning...");
                StatusReporter.setFieldAgentStatus().setControllerStatus(NOT_PROVISIONED);
                deProvision(false);
            } catch (Exception e) {}
        }

        try {
            provisioningLock.lock();
            provisioningResult = orchestrator.provision(key);

            microserviceManager.clear();
            try{
                ProcessManager.getInstance().deleteRemainingMicroservices();
            } catch (Exception e) {
                logError("Error deleting remaining microservices",
                        new AgentSystemException(e.getMessage(), e));
            }
            StatusReporter.setFieldAgentStatus().setControllerStatus(OK);
            Configuration.setIofogUuid(provisioningResult.getString("uuid"));
            Configuration.setAccessToken(provisioningResult.getString("token"));

            Configuration.saveConfigUpdates();
            Configuration.updateConfigBackUpFile();

            postFogConfig();
            loadRegistries(false);
            List<Microservice> microservices = loadMicroservices(false);
            processMicroserviceConfig(microservices);
            processRoutes(microservices);
            notifyModules();
            loadEdgeResources(false);

            sendHWInfoFromHalToController();

            postStatusHelper();

            logInfo("Provisioning success");

        } catch (CertificateException | SSLHandshakeException e) {
            verificationFailed(e);
            provisioningResult = buildProvisionFailResponse("Certificate error", e);
        } catch (UnknownHostException e) {
            StatusReporter.setFieldAgentStatus().setControllerVerified(false);
            provisioningResult = buildProvisionFailResponse("Connection error: unable to connect to fog controller.", e);
        } catch (Exception e) {
            provisioningResult = buildProvisionFailResponse(e.getMessage(), e);
        } finally {
            provisioningLock.unlock();
        }
        return provisioningResult;
    }

    private JsonObject buildProvisionFailResponse(String message, Exception e) {
        logError("Provisioning failed",
        		new AgentSystemException("Provisioning failed : " + message, e));
        return Json.createObjectBuilder()
                .add("status", "failed")
                .add("errorMessage", message)
                .build();
    }

    /**
     * notifies other modules
     */
    private void notifyModules() {
    	logInfo("Notifying modules for configuration update");
    	try {
            MessageBus.getInstance().update();
        } catch (Exception e) {
    	    logWarning("Unable to update Message Bus" + " : " + e.getMessage());
        }
        LocalApi.getInstance().update();
        ProcessManager.getInstance().update();
    }

    /**
     * does de-provisioning
     *
     * @return String
     */
    public String deProvision(boolean isTokenExpired) {
        logInfo("Start Deprovisioning");

        if (!provisioningLock.tryLock()) {
            String msg = "Provisioning in progress";
            logInfo(msg);
            return msg;
        }

        try {
            if (notProvisioned()) {
                logInfo("Finished Deprovisioning : Failure - not provisioned");
                return "\nFailure - not provisioned";
            }

            if (!isTokenExpired) {
                try {
                    orchestrator.request("deprovision", RequestType.POST, null, getDeprovisionBody());
                } catch (CertificateException | SSLHandshakeException e) {
                    verificationFailed(e);
                    logError("Unable to make deprovision request due to broken certificate ",
                            new AgentSystemException(e.getMessage(), e));
                } catch (Exception e) {
                    logError("Unable to make deprovision request ",
                            new AgentSystemException(e.getMessage(), e));
                }
            }

            StatusReporter.setFieldAgentStatus().setControllerStatus(NOT_PROVISIONED);
            String iofogUuid = Configuration.getIofogUuid();
            boolean configUpdated = true;
            try {
                Configuration.setIofogUuid("");
                Configuration.setAccessToken("");
                Configuration.saveConfigUpdates();
            } catch (Exception e) {
                configUpdated = false;
                try {
                    logError("Error saving config updates", new AgentSystemException("Error saving config updates", e));
                } catch (Exception ex){
                    logError("This error should not print ever!", new AgentSystemException("Error Logging exception in saving config updates on deprovision"));
                }
            } finally {
                if (configUpdated) {
                    Configuration.updateConfigBackUpFile();
                }
            }
            microserviceManager.clear();
            try {
                ProcessManager.getInstance().stopRunningMicroservices(false, iofogUuid);
            } catch (Exception e) {
                logError("Error stopping running microservices",
                        new AgentSystemException(e.getMessage(), e));
            }
            notifyModules();
            logInfo("Finished Deprovisioning : Success - tokens, identifiers and keys removed");
        } finally {
            provisioningLock.unlock();
        }
        return "\nSuccess - tokens, identifiers and keys removed";
    }

    private JsonObject getDeprovisionBody() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        Set<String> microserviceUuids = Stream.concat(
            microserviceManager.getLatestMicroservices().stream(),
            microserviceManager.getCurrentMicroservices().stream()
        )
            .map(Microservice::getMicroserviceUuid)
            .collect(Collectors.toSet());

        microserviceUuids.forEach(arrayBuilder::add);

        return Json.createObjectBuilder()
            .add("microserviceUuids", arrayBuilder)
            .build();
    }

    /**
     * sends IOFog configuration when any changes applied
     */
    public void instanceConfigUpdated() {
        logDebug("Start IOFog configuration update");
        try {
            postFogConfig();
        } catch (Exception e) {
            logError("Error posting updated for config ", e);
        }
            orchestrator.update();
        logDebug("Finished IOFog configuration update");
    }

    /**
     * starts Field Agent module
     */
    public void start() {
        logDebug("Start the Field Agent");
        if (isNullOrEmpty(Configuration.getIofogUuid()) || isNullOrEmpty(Configuration.getAccessToken()))
            StatusReporter.setFieldAgentStatus().setControllerStatus(NOT_PROVISIONED);

        microserviceManager = MicroserviceManager.getInstance();
        orchestrator = new Orchestrator();
        sshProxyManager = new SshProxyManager(new SshConnection());
        edgeResourceManager = EdgeResourceManager.getInstance();

        boolean isConnected = ping();
        getFogConfig();
        if (!notProvisioned()) {
            loadRegistries(!isConnected);
            List<Microservice> microservices = loadMicroservices(!isConnected);
            processMicroserviceConfig(microservices);
            processRoutes(microservices);
            loadEdgeResources(!isConnected);
        }

        new Thread(pingController, Constants.FIELD_AGENT_PING_CONTROLLER).start();
        new Thread(getChangesList, Constants.FIELD_AGENT_GET_CHANGE_LIST).start();
        new Thread(postStatus, Constants.FIELD_AGENT_POST_STATUS).start();
        new Thread(postDiagnostics, Constants.FIELD_AGENT_POST_DIAGNOSTIC).start();

        StatusReporter.setFieldAgentStatus().setReadyToUpgrade(VersionHandler.isReadyToUpgrade());
        StatusReporter.setFieldAgentStatus().setReadyToRollback(VersionHandler.isReadyToRollback());
        futureTask = scheduler.scheduleAtFixedRate(getAgentReadyToUpgradeStatus, 0, Configuration.getReadyToUpgradeScanFrequency(), TimeUnit.HOURS);
        logDebug("Field Agent started");
    }

    /**
     * checks if IOFog controller connection is broken
     *
     * @param fromFile
     * @return boolean
     */
    private boolean isControllerConnected(boolean fromFile) {
    	logDebug("check is Controller Connected");
        boolean isConnected = false;
        if ((!StatusReporter.getFieldAgentStatus().getControllerStatus().equals(OK) && !ping()) && !fromFile) {
            handleBadControllerStatus();
        } else {
            isConnected = true;
        }
        logDebug(String.format("checked is Controller Connected : %s ", isConnected) );
        return isConnected;
    }

    private void handleBadControllerStatus() {
    	logDebug("Start handle Bad Controller Status");
        String errMsg = "Connection to controller has broken";
        if (StatusReporter.getFieldAgentStatus().isControllerVerified()) {
            logWarning(errMsg);
        } else {
            verificationFailed(new Exception(errMsg));
        }
        logDebug("Finished handling Bad Controller Status");
    }

    public void sendUSBInfoFromHalToController() {
    	logDebug("Start send USB Info from hal To Controller");
        if (notProvisioned()) {
            return;
        }
        Optional<StringBuilder> response = getResponse(USB_INFO_URL);
        if (isResponseValid(response)) {
            String usbInfo = response.get().toString();
            StatusReporter.setResourceManagerStatus().setUsbConnectionsInfo(usbInfo);

            JsonObject json = Json.createObjectBuilder()
                    .add("info", usbInfo)
                    .build();
            try {
                orchestrator.request(COMMAND_USB_INFO, RequestType.PUT, null, json);
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error while sending USBInfo from hal to controller",
                		new AgentSystemException(e.getMessage(), e));
            }
        }
        logDebug("Finished send USB Info from hal To Controller");
    }

    public void sendHWInfoFromHalToController() {
    	logDebug("Start send HW Info from HAL To Controller");
        if (notProvisioned()) {
            return;
        }
        Optional<StringBuilder> response = getResponse(HW_INFO_URL);
        if (isResponseValid(response)) {
            String hwInfo = response.get().toString();
            StatusReporter.setResourceManagerStatus().setHwInfo(hwInfo);

            JsonObject json = Json.createObjectBuilder()
                    .add("info", hwInfo)
                    .build();

            JsonObject jsonSendHWInfoResult = null;
            try {
                jsonSendHWInfoResult = orchestrator.request(COMMAND_HW_INFO, RequestType.PUT, null, json);
            } catch (Exception e) {
            	LoggingService.logError(MODULE_NAME, "Error while sending HW Info from hal to controller",
                		new AgentSystemException(e.getMessage(), e));
            }

            if (jsonSendHWInfoResult == null) {
                LoggingService.logInfo(MODULE_NAME, "Can't get HW Info from HAL.");
            }
        }
        logDebug("Finished send HW Info from HAL To Controller");
    }

    private boolean isResponseValid(Optional<StringBuilder> response) {
        return response.isPresent() && !response.get().toString().isEmpty();
    }

    private Optional<StringBuilder> getResponse(String spec) {
    	logDebug("Start get response");
        Optional<HttpURLConnection> connection = sendHttpGetReq(spec);
        StringBuilder content = null;
        if (connection.isPresent()) {
            content = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.get().getInputStream(), UTF_8))) {
                String inputLine;
                content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
            } catch (IOException exc) {
                logDebug("HAL is not enabled for this Iofog Agent at the moment");
            }
            connection.get().disconnect();
        }
        logDebug("Finished get response");
        return Optional.ofNullable(content);
    }

    private Optional<HttpURLConnection> sendHttpGetReq(String spec) {
    	logDebug("Start sending Http request");
        HttpURLConnection connection;
        try {
            URL url = new URL(spec);
            connection = (HttpURLConnection) url.openConnection();
            if(connection != null){
                connection.setRequestMethod(HttpMethod.GET);
                connection.getResponseCode();
            }
        } catch (IOException exc) {
            connection = null;
            logDebug("HAL is not enabled for this Iofog Agent at the moment");
        }
        logDebug("Finished sending Http request");
        return Optional.ofNullable(connection);
    }

    private void createImageSnapshot() {
        if (notProvisioned() || !isControllerConnected(false)) {
            return;
        }

        LoggingService.logDebug(MODULE_NAME, "Create image snapshot");

        String microserviceUuid = null;

        try {
            JsonObject jsonObject = orchestrator.request("image-snapshot", RequestType.GET, null, null);
            microserviceUuid = jsonObject.getString("uuid");
        } catch (Exception e) {
        	LoggingService.logError(MODULE_NAME, "Unable get name of image snapshot",
            		new AgentSystemException(e.getMessage(), e));
        }

        if (SystemUtils.IS_OS_WINDOWS) {
            return; // TODO implement
        }

        if (microserviceUuid != null) {
            ImageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        }
        LoggingService.logDebug(MODULE_NAME, "Finished Create image snapshot");
    }
    /**
     * returns report for "info" about ready to upgrade and ready to rollback
     *
     * @return info getCheckUpgradeReadyReport
     */
    public String getCheckUpgradeReadyReport() {
        LoggingService.logDebug(MODULE_NAME, "Start get upgrade ready report");

        StringBuilder result = new StringBuilder();
        boolean isReadyToUpgrade = VersionHandler.isReadyToUpgrade();
        boolean isReadyToRollback = VersionHandler.isReadyToRollback();

        // isReadyToUpgrade
        result.append(buildReportLine("Ready To Upgrade", String.valueOf(isReadyToUpgrade)));
        // isReadyToRollback
        result.append(buildReportLine("Ready To Rollback", String.valueOf(isReadyToRollback)));

        LoggingService.logInfo(MODULE_NAME, "Finished get upgrade ready report");

        return result.toString();
    }

    private String buildReportLine(String messageDescription, String value) {
        return rightPad(messageDescription, 40, ' ') + " : " + value + "\\n";
    }

    /**
     *  get isReadyToUpgrade and isReadyToRollback Status
     */
    private Runnable getAgentReadyToUpgradeStatus = () -> {
        LoggingService.logDebug(MODULE_NAME, "Start scan of isReadyToUpgrade and isReadyToRollback Status");
        try {
            boolean isReadyToRollback = VersionHandler.isReadyToRollback();
            boolean isReadyToUpgrade = VersionHandler.isReadyToUpgrade();
            StatusReporter.setFieldAgentStatus().setReadyToRollback(isReadyToRollback);
            StatusReporter.setFieldAgentStatus().setReadyToUpgrade(isReadyToUpgrade);
        } catch (Exception e){
            LoggingService.logError(MODULE_NAME,"Error getting isReadyToUpgrade and isReadyToRollback Status", new AgentSystemException(e.getMessage(), e));
        }
        LoggingService.logDebug(MODULE_NAME, "Finished scan of isReadyToUpgrade and isReadyToRollback Status");

    };
    /**
     * This method will reschedule "myTask" with the new param time
     */
    public void changeReadInterval()
    {
        if (futureTask != null)
        {
            futureTask.cancel(true);
        }
        futureTask = scheduler.scheduleAtFixedRate(getAgentReadyToUpgradeStatus, 0, Configuration.getReadyToUpgradeScanFrequency(), TimeUnit.HOURS);
    }
}