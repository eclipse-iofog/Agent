/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
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
import org.apache.commons.lang3.SystemUtils;
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
import org.eclipse.iofog.microservice.*;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.proxy.SshConnection;
import org.eclipse.iofog.proxy.SshProxyManager;
import org.eclipse.iofog.pruning.DockerPruningManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Constants.ControllerStatus;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.JwtManager;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.functional.Pair;
import org.eclipse.iofog.gps.GpsManager;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.volume_mount.VolumeMountManager;
import org.eclipse.iofog.process_manager.ExecSessionCallback;
import org.eclipse.iofog.process_manager.ExecSessionStatus;
import org.eclipse.iofog.utils.ExecSessionWebSocketHandler;

import jakarta.json.*;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.netty.util.internal.StringUtil.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.rightPad;
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
    private ExecSessionWebSocketHandler execSessionWebSocketHandler;
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
    private VolumeMountManager volumeMountManager;
    private LogSessionManager logSessionManager;

    private final Map<String, String> activeExecSessions = new ConcurrentHashMap<>();
    private final Map<String, ExecSessionCallback> execCallbacks = new ConcurrentHashMap<>();
    private final Map<String, ExecSessionWebSocketHandler> activeWebSockets = new ConcurrentHashMap<>();
    private final Map<String, ExecSessionCallback> activeExecCallbacks = new ConcurrentHashMap<>();

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
                .add("warningMessage", StatusReporter.getSupervisorStatus().getWarningMessage() == null ?
                        "" : StatusReporter.getSupervisorStatus().getWarningMessage())
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
                .add("lastCommandTime", StatusReporter.getFieldAgentStatus().getLastCommandTime())
                .add("tunnelStatus", StatusReporter.getSshManagerStatus().getJsonProxyStatus() == null ?
                        "UNKNOWN" : StatusReporter.getSshManagerStatus().getJsonProxyStatus())
                .add("version", getVersion() == null ?
                        "UNKNOWN" : getVersion())
                .add("isReadyToUpgrade", StatusReporter.getFieldAgentStatus().isReadyToUpgrade())
                .add("isReadyToRollback", StatusReporter.getFieldAgentStatus().isReadyToRollback())
                .add("activeVolumeMounts", StatusReporter.getVolumeMountManagerStatus().getActiveMounts())
                .add("volumeMountLastUpdate", StatusReporter.getVolumeMountManagerStatus().getLastUpdate())
                .add("gpsStatus", GpsManager.getInstance().getStatus().getHealthStatus().name())
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
     * posts ioFog status to ioFog controller
     */
    public void postStatusHelper() {
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
        logDebug("Starting processChanges with changes: " + changes.toString());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            boolean resetChanges = true;
            logDebug("Processing changes with initialization flag: " + initialization);

            if (changes.getBoolean("deleteNode",false) && !initialization) {
                logDebug("Processing deleteNode change");
                try {
                    deleteNode();
                } catch (Exception e) {
                    logError("Unable to delete node", e);
                    resetChanges = false;
                }
            } else {
                if (changes.getBoolean("reboot",false) && !initialization) {
                    logDebug("Processing reboot change");
                    try {
                        reboot();
                    } catch (Exception e) {
                        logError("Unable to perform reboot", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("isImageSnapshot",false) && !initialization) {
                    logDebug("Processing imageSnapshot change");
                    try {
                        createImageSnapshot();
                    } catch (Exception e) {
                        logError("Unable to create snapshot", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("config",false) && !initialization) {
                    logDebug("Processing config change");
                    try {
                        getFogConfig();
                    } catch (Exception e) {
                        logError("Unable to get config", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("version",false) && !initialization) {
                    logDebug("Processing version change");
                    try {
                        changeVersion();
                    } catch (Exception e) {
                        logError("Unable to change version", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("registries",false) || initialization) {
                    logDebug("Processing registries change");
                    try {
                        loadRegistries(false);
                        ProcessManager.getInstance().update();
                    } catch (Exception e) {
                        logError("Unable to update registries", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("prune", false) && !initialization) {
                    logDebug("Processing prune change");
                    try {
                        DockerPruningManager.getInstance().pruneAgent();
                    } catch (Exception e) {
                        logError("Unable to prune Agent", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("volumeMounts",false) || initialization) {
                    logDebug("Processing volumeMounts change");
                    loadVolumeMounts();
                }
                if (changes.getBoolean("microserviceConfig",false) || changes.getBoolean("microserviceList",false) ||
                        changes.getBoolean("execSessions",false) || initialization) {
                    logDebug("Processing microservice related changes - microserviceConfig: " + changes.getBoolean("microserviceConfig",false) + 
                            ", microserviceList: " + changes.getBoolean("microserviceList",false) + 
                            ", execSessions: " + changes.getBoolean("execSessions",false));
                    logDebug("Changes object structure: " + changes.toString());
                    boolean microserviceConfig = changes.getBoolean("microserviceConfig");
                    boolean execSessions = changes.getBoolean("execSessions");
                    int defaultFreq = Configuration.getStatusFrequency();
                    Configuration.setStatusFrequency(1);
                    try {
                        List<Microservice> microservices = loadMicroservices(false);
                        logDebug("Loaded " + microservices.size() + " microservices");

                        if (microserviceConfig) {
                            logDebug("Processing microservice config changes");
                            try {
                                processMicroserviceConfig(microservices);
                                LocalApi.getInstance().update();
                            } catch (Exception e) {
                                logError("Unable to update microservices config", e);
                                resetChanges = false;
                            }
                        }

                        // Notify ProcessManager to immediately restart monitoring thread
                        // This ensures containers are processed without waiting for the next scheduled interval
                        ProcessManager.getInstance().update();

                        if (execSessions) {
                            logDebug("Processing exec sessions changes");
                            try {
                                handleExecSessions(microservices);
                            } catch (Exception e) {
                                logError("Unable to handle exec sessions", e);
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
                    logDebug("Processing tunnel change");
                    try {
                        sshProxyManager.update(getProxyConfig());
                    } catch (Exception e) {
                        logError("Unable to create tunnel", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("diagnostics",false) && !initialization) {
                    logDebug("Processing diagnostics change");
                    try {
                        updateDiagnostics();
                    } catch (Exception e) {
                        logError("Unable to update diagnostics", e);
                        resetChanges = false;
                    }
                }
                if (changes.getBoolean("linkedEdgeResources",false) && !initialization) {
                    logDebug("Processing linkedEdgeResources change");
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
                if (changes.getBoolean("microserviceLogs", false) || changes.getBoolean("fogLogs", false)) {
                    logDebug("Processing log sessions changes - microserviceLogs: " + changes.getBoolean("microserviceLogs", false) + 
                        ", fogLogs: " + changes.getBoolean("fogLogs", false));
                    try {
                        handleLogSessions();
                    } catch (Exception e) {
                        logError("Unable to handle log sessions", e);
                        resetChanges = false;
                    }
                }
            }
            logDebug("Finished processing changes with resetChanges: " + resetChanges);
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
                    logDebug("Requesting changes from controller");
                    result = orchestrator.request("config/changes", RequestType.GET, null, null);
                    logDebug("Received changes from controller: " + result.toString());
                } catch (CertificateException | SSLHandshakeException e) {
                    verificationFailed(e);
                    logError("Unable to get changes due to broken certificate",
                            new AgentSystemException(e.getMessage(), e));
                    continue;
                } catch (SocketTimeoutException e) {
                    logDebug("Socket timeout while getting changes, updating network interface");
                    IOFogNetworkInterfaceManager.getInstance().updateIOFogNetworkInterface();
                    continue;
                } catch (Exception e) {
                    logError("Unable to get changes ", new AgentSystemException(e.getMessage(), e));
                    continue;
                }

                StatusReporter.setFieldAgentStatus().setLastCommandTime(lastGetChangesList);

                String lastUpdated = result.getString("lastUpdated", null);
                logDebug("Processing changes with lastUpdated: " + lastUpdated);
                boolean resetChanges;
                Future<Boolean> changesProcessor = processChanges(result);

                try {
                    logDebug("Waiting for changes processing to complete");
                    resetChanges = changesProcessor.get(30, TimeUnit.SECONDS);
                    logDebug("Changes processing completed with resetChanges: " + resetChanges);
                } catch (Exception e) {
                    logError("Error waiting for changes processing", e);
                    resetChanges = false;
                    changesProcessor.cancel(true);
                }

                if (lastUpdated != null && resetChanges) {
                    logDebug("Resetting config changes flags with lastUpdated: " + lastUpdated);
                    try {
                        JsonObject req = Json.createObjectBuilder()
                                .add("lastUpdated", lastUpdated)
                                .build();
                        orchestrator.request("config/changes", RequestType.PATCH, null, req);
                        logDebug("Successfully reset config changes flags");
                    } catch (Exception e) {
                        logError("Resetting config changes has failed", e);
                    }
                }

                initialization = initialization && !resetChanges;
                logDebug("Finished getChangesList cycle with initialization: " + initialization);
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
        deProvision(true);
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

        String ioFogDaemon = System.getenv("IOFOG_DAEMON");
		if ("container".equals(ioFogDaemon)) {
            logWarning("Skipping reboot as iofog-agent running inside container");
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

    private JsonArray loadMicroservicesJsonFile() {
        String filename = MICROSERVICE_FILE;
        JsonArray microservicesJson = readFile(filesPath + filename);
        return  microservicesJson;
    }

    /**
     * gets list of VolumeMounts from IOFog controller
     */
    private void loadVolumeMounts() {
        try {
            JsonObject result = orchestrator.request("volumeMounts", RequestType.GET, null, null);
            if (result.containsKey("volumeMounts")) {
                JsonArray volumeMounts = result.getJsonArray("volumeMounts");
                volumeMountManager.processVolumeMountChanges(volumeMounts);
            }
        } catch (Exception e) {
            logError("Unable to process volume mount changes", e);
        }
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
            Microservice microservice = new Microservice(jsonObj.getString("uuid"), jsonObj.getString("imageId"), jsonObj.getString("name"), jsonObj.getString("application"));
            microservice.setConfig(jsonObj.getString("config"));
            if (!jsonObj.isNull("runAsUser")) {
                microservice.setRunAsUser(jsonObj.getString("runAsUser"));
            }
            if (!jsonObj.isNull("platform")) {
                microservice.setPlatform(jsonObj.getString("platform"));
            }
    
            if (!jsonObj.isNull("runtime")) {
                microservice.setRuntime(jsonObj.getString("runtime"));
            }
            microservice.setRebuild(jsonObj.getBoolean("rebuild"));
            microservice.setHostNetworkMode(jsonObj.getBoolean("hostNetworkMode"));
            microservice.setIsPrivileged(jsonObj.getBoolean("isPrivileged"));
            microservice.setRegistryId(jsonObj.getInt("registryId"));
            microservice.setSchedule(jsonObj.getInt("schedule"));
            microservice.setLogSize(jsonObj.getJsonNumber("logSize").longValue());
            microservice.setDelete(jsonObj.getBoolean("delete"));
            microservice.setDeleteWithCleanup(jsonObj.getBoolean("deleteWithCleanup"));

            microservice.setRouter(jsonObj.getBoolean("isRouter"));
            microservice.setNats(jsonObj.getBoolean("isNats"));
            if (jsonObj.getBoolean("isRouter")) {
                Configuration.setRouterUuid(jsonObj.getString("uuid"));
                Configuration.setRouterInterior(jsonObj.getBoolean("hostNetworkMode"));
            }
            microservice.setExecEnabled(jsonObj.getBoolean("execEnabled"));

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
                            VolumeMappingType volumeMappingType;
                            String typeStr = volumeMapping.getString("type", "bind");
                            if ("volumeMount".equals(typeStr)) {
                                volumeMappingType = VolumeMappingType.VOLUME_MOUNT;
                            } else if ("volume".equals(typeStr)) {
                                volumeMappingType = VolumeMappingType.VOLUME;
                            } else {
                                volumeMappingType = VolumeMappingType.BIND;
                            }
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

            JsonValue cdiDevsValue = jsonObj.get("cdiDevices");
            microservice.setCdiDevs(getStringList(cdiDevsValue));

            if (!jsonObj.isNull("annotations")) {
                microservice.setAnnotations(jsonObj.getString("annotations"));
            }

            JsonValue capAddValue = jsonObj.get("capAdd");
            microservice.setCapAdd(getStringList(capAddValue));

            JsonValue capDropValue = jsonObj.get("capDrop");
            microservice.setCapDrop(getStringList(capDropValue));

            JsonValue extraHostsValue = jsonObj.get("extraHosts");
            microservice.setExtraHosts(getStringList(extraHostsValue));

            if (!jsonObj.isNull("pidMode")) {
                microservice.setPidMode(jsonObj.getString("pidMode"));
            }
            if (!jsonObj.isNull("ipcMode")) {
                microservice.setIpcMode(jsonObj.getString("ipcMode"));
            }
            if (!jsonObj.isNull("cpuSetCpus")) {
                microservice.setCpuSetCpus(jsonObj.getString("cpuSetCpus"));
            }

            JsonValue healthcheckValue = jsonObj.get("healthCheck");
            if (healthcheckValue != null && !healthcheckValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                JsonObject healthcheckObj = (JsonObject) healthcheckValue;
                JsonValue testValue = healthcheckObj.get("test");
                List<String> testList = getStringList(testValue);
                
                // Handle null values for numeric fields
                Long interval = healthcheckObj.containsKey("interval") && !healthcheckObj.isNull("interval") ? 
                    healthcheckObj.getJsonNumber("interval").longValue() : null;
                Long timeout = healthcheckObj.containsKey("timeout") && !healthcheckObj.isNull("timeout") ? 
                    healthcheckObj.getJsonNumber("timeout").longValue() : null;
                Long startPeriod = healthcheckObj.containsKey("startPeriod") && !healthcheckObj.isNull("startPeriod") ? 
                    healthcheckObj.getJsonNumber("startPeriod").longValue() : null;
                Long startInterval = healthcheckObj.containsKey("startInterval") && !healthcheckObj.isNull("startInterval") ? 
                    healthcheckObj.getJsonNumber("startInterval").longValue() : null;
                Integer retries = healthcheckObj.containsKey("retries") && !healthcheckObj.isNull("retries") ? 
                    healthcheckObj.getInt("retries") : null;
                
                microservice.setHealthcheck(new Healthcheck(testList, interval, timeout, startPeriod, startInterval, retries));
            }

            if (jsonObj.containsKey("memoryLimit") && !jsonObj.isNull("memoryLimit")) {
                microservice.setMemoryLimit(jsonObj.getJsonNumber("memoryLimit").longValue());
            }

            JsonValue serviceAccountValue = jsonObj.get("serviceAccount");
            if (serviceAccountValue != null && !serviceAccountValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                JsonObject serviceAccountObj = (JsonObject) serviceAccountValue;
                String serviceAccountName = serviceAccountObj.containsKey("name") && !serviceAccountObj.isNull("name") 
                    ? serviceAccountObj.getString("name") : null;
                
                RoleRef roleRef = null;
                JsonValue roleRefValue = serviceAccountObj.get("roleRef");
                if (roleRefValue != null && !roleRefValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                    JsonObject roleRefObj = (JsonObject) roleRefValue;
                    String kind = roleRefObj.containsKey("kind") && !roleRefObj.isNull("kind") 
                        ? roleRefObj.getString("kind") : null;
                    String name = roleRefObj.containsKey("name") && !roleRefObj.isNull("name") 
                        ? roleRefObj.getString("name") : null;
                    if (kind != null && name != null) {
                        roleRef = new RoleRef(kind, name);
                    }
                }
                
                List<Rule> rules = null;
                JsonValue rulesValue = serviceAccountObj.get("rules");
                if (rulesValue != null && !rulesValue.getValueType().equals(JsonValue.ValueType.NULL)) {
                    JsonArray rulesArray = (JsonArray) rulesValue;
                    if (rulesArray.size() > 0) {
                        rules = IntStream.range(0, rulesArray.size())
                            .boxed()
                            .map(rulesArray::getJsonObject)
                            .map(ruleObj -> {
                                List<String> apiGroups = getStringList(ruleObj.get("apiGroups"));
                                List<String> resources = getStringList(ruleObj.get("resources"));
                                List<String> verbs = getStringList(ruleObj.get("verbs"));
                                return new Rule(apiGroups, resources, verbs);
                            })
                            .collect(toList());
                    }
                }
                
                if (serviceAccountName != null || roleRef != null || rules != null) {
                    microservice.setServiceAccount(new ServiceAccount(serviceAccountName, roleRef, rules));
                }
            }

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
                .add("timestamp", System.currentTimeMillis())
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
                int edgeGuardFrequency = configs.containsKey(EDGE_GUARD_FREQUENCY.getJsonProperty()) ?
                        configs.getInt(EDGE_GUARD_FREQUENCY.getJsonProperty()) :
                        Integer.parseInt(EDGE_GUARD_FREQUENCY.getDefaultValue());
                String gpsDevice = configs.containsKey(GPS_DEVICE.getJsonProperty()) ?
                        configs.getString(GPS_DEVICE.getJsonProperty()) :
                        GPS_DEVICE.getDefaultValue();
                int gpsScanFrequency = configs.containsKey(GPS_SCAN_FREQUENCY.getJsonProperty()) ?
                        configs.getInt(GPS_SCAN_FREQUENCY.getJsonProperty()) :
                        Integer.parseInt(GPS_SCAN_FREQUENCY.getDefaultValue());
                String gpsMode = configs.containsKey(GPS_MODE.getJsonProperty()) ?
                        configs.getString(GPS_MODE.getJsonProperty()) :
                        GPS_MODE.getDefaultValue();
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

                if ((Configuration.getEdgeGuardFrequency() != edgeGuardFrequency) && (edgeGuardFrequency >= 0))
                instanceConfig.put(EDGE_GUARD_FREQUENCY.getCommandName(), edgeGuardFrequency);

                if (Configuration.getGpsDevice() != gpsDevice)
                    instanceConfig.put(GPS_DEVICE.getCommandName(), gpsDevice);

                if (!Configuration.getGpsMode().equals(gpsMode))
                    instanceConfig.put(GPS_MODE.getCommandName(), gpsMode);

                if (Configuration.getGpsScanFrequency() != gpsScanFrequency)
                    instanceConfig.put(GPS_SCAN_FREQUENCY.getCommandName(), gpsScanFrequency);

                if (Configuration.getGpsCoordinates() != null && !Configuration.getGpsCoordinates().equals(gpsCoordinates)) {
                    instanceConfig.put(GPS_MODE.getCommandName(), gpsCoordinates);
                }

                if (Configuration.getGpsCoordinates() == null && !gpsCoordinates.equals("0.0,0.0")) {
                    instanceConfig.put(GPS_MODE.getCommandName(), gpsCoordinates);
                }

                if (Configuration.getLogLevel() != null && !Configuration.getLogLevel().equals(logLevel))
                    instanceConfig.put(LOG_LEVEL.getCommandName(), logLevel);

                if ((Configuration.getDockerPruningFrequency() != dockerPruningFrequency) && (dockerPruningFrequency >= 0))
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
                .add(EDGE_GUARD_FREQUENCY.getJsonProperty(), Configuration.getEdgeGuardFrequency())
                .add(GPS_DEVICE.getJsonProperty(), Configuration.getGpsDevice())
                .add(GPS_SCAN_FREQUENCY.getJsonProperty(), Configuration.getGpsScanFrequency())
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
     * sends IOFog instance GPSconfiguration to IOFog controller
     */
    private void postGpsConfig() {
        logInfo("Post ioFog GPS config");
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

        JsonObject json = Json.createObjectBuilder()
                .add("latitude", latitude)
                .add("longitude", longitude)
                .build();

        try {
            orchestrator.request("config/gps", RequestType.PATCH, null, json);
        } catch (CertificateException | SSLHandshakeException e) {
            verificationFailed(e);
            logError("Unable to post ioFog GPS config due to broken certificate ",
            		new AgentSystemException(e.getMessage(), e));
        } catch (Exception e) {
            logError("Unable to post ioFog GPS config ", new AgentSystemException(e.getMessage(), e));
        }
        logInfo("Finished Post ioFog GPS config");
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

        // Check if already provisioned
        if (!notProvisioned()) {
            try {
                logInfo("Agent is already provisioned. Deprovisioning...");
                StatusReporter.setFieldAgentStatus().setControllerStatus(NOT_PROVISIONED);
                deProvision(false);
            } catch (Exception e) {
                logError("Error during deprovisioning", e);
                return buildProvisionFailResponse("Error during deprovisioning", e);
            }
        }

        // Reset JWT Manager to ensure clean state for new provisioning
        try {
            JwtManager.reset();
            logDebug("JWT Manager reset for new provisioning");
        } catch (Exception e) {
            logWarning("Failed to reset JWT Manager before provisioning: " + e.getMessage());
            // Continue with provisioning even if JWT reset fails
        }

        try {
            // Try to acquire lock - if we can't get it, provisioning is already in progress
            if (!provisioningLock.tryLock()) {
                logWarning("Provisioning already in progress");
                return buildProvisionFailResponse("Provisioning already in progress", null);
            }

            try {
                // Perform provisioning
                provisioningResult = orchestrator.provision(key);
                
                // Clear existing state
                microserviceManager.clear();
                try {
                    ProcessManager.getInstance().deleteRemainingMicroservices();
                } catch (Exception e) {
                    logError("Error deleting remaining microservices", e);
                }

                // Set initial configuration
                Configuration.setIofogUuid(provisioningResult.getString("uuid"));
                Configuration.setPrivateKey(provisioningResult.getString("privateKey"));
                Configuration.setNamespace(provisioningResult.getString("namespace"));
                Configuration.saveConfigUpdates();
                Configuration.updateConfigBackUpFile();

                // Verify JWT generation works
                try {
                    if (JwtManager.generateJwt() == null) {
                        logError("Failed to initialize JWT Manager", new AgentSystemException("Failed to initialize JWT Manager"));
                        // Clean up on JWT failure
                        Configuration.setIofogUuid("");
                        Configuration.setPrivateKey("");
                        Configuration.saveConfigUpdates();
                        StatusReporter.setFieldAgentStatus().setControllerStatus(NOT_PROVISIONED);
                        return buildProvisionFailResponse("Failed to initialize JWT Manager - Missing required dependencies", null);
                    }
                } catch (NoClassDefFoundError e) {
                    logError("Missing required dependencies for JWT generation", new AgentSystemException(e.getMessage(), e));
                    // Clean up on dependency error
                    Configuration.setIofogUuid("");
                    Configuration.setPrivateKey("");
                    Configuration.saveConfigUpdates();
                    StatusReporter.setFieldAgentStatus().setControllerStatus(NOT_PROVISIONED);
                    return buildProvisionFailResponse("Missing required dependencies for JWT generation", new AgentSystemException(e.getMessage(), e));
                }

                // Set status to OK since provisioning succeeded
                StatusReporter.setFieldAgentStatus().setControllerStatus(OK);

                // Only do essential post-provisioning operations
                try {
                    postFogConfig();
                } catch (Exception e) {
                    logError("Error posting fog config", e);
                    // Don't fail provisioning for this
                }

                logInfo("Provisioning success");
                return provisioningResult;

            } finally {
                provisioningLock.unlock();
            }

        } catch (CertificateException | SSLHandshakeException e) {
            verificationFailed(e);
            return buildProvisionFailResponse("Certificate error", e);
        } catch (UnknownHostException e) {
            StatusReporter.setFieldAgentStatus().setControllerVerified(false);
            return buildProvisionFailResponse("Connection error: unable to connect to fog controller.", e);
        } catch (Exception e) {
            return buildProvisionFailResponse(e.getMessage(), e);
        }
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

            // Store configuration values before clearing them
            String iofogUuid = Configuration.getIofogUuid();
            String privateKey = Configuration.getPrivateKey();
            String namespace = Configuration.getNamespace();
            
            // Attempt deprovision request if not token expired
            boolean deprovisionRequestSuccessful = false;
            if (!isTokenExpired) {
                try {
                    logDebug("Attempting deprovision request to controller");
                    orchestrator.request("deprovision", RequestType.POST, null, getDeprovisionBody());
                    logInfo("Deprovision request completed successfully");
                    deprovisionRequestSuccessful = true;
                } catch (CertificateException | SSLHandshakeException e) {
                    verificationFailed(e);
                    logError("Unable to make deprovision request due to broken certificate ",
                            new AgentSystemException(e.getMessage(), e));
                } catch (Exception e) {
                    logError("Unable to make deprovision request ",
                            new AgentSystemException(e.getMessage(), e));
                }
            } else {
                // If token is expired, we skip the deprovision request
                logInfo("Skipping deprovision request due to expired token");
            }

            // Update status to NOT_PROVISIONED
            StatusReporter.setFieldAgentStatus().setControllerStatus(NOT_PROVISIONED);
            
            // Clear configuration AFTER the deprovision request attempt
            boolean configUpdated = true;
            try {
                Configuration.setIofogUuid("");
                // Configuration.setAccessToken("");
                Configuration.setPrivateKey("");
                Configuration.saveConfigUpdates();
                Configuration.setNamespace("default");
                logDebug("Configuration cleared successfully");
                
                // Reset JWT Manager to clear static state and allow re-initialization with new credentials
                try {
                    JwtManager.reset();
                    logDebug("JWT Manager reset completed");
                } catch (Exception e) {
                    logWarning("Failed to reset JWT Manager: " + e.getMessage());
                    // Don't fail deprovisioning for JWT reset failure
                }
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
            
            // Clear microservice manager
            microserviceManager.clear();
            
            // Stop and remove all agent containers (and volumes) so no sensitive data remains
            try {
                ProcessManager.getInstance().stopRunningMicroservices(true, iofogUuid);
            } catch (Exception e) {
                logError("Error stopping running microservices",
                        new AgentSystemException(e.getMessage(), e));
            }
            
            // Clear volume mounts
            try {
                volumeMountManager.clear();
            } catch (Exception e) {
                logError("Error clearing volume mounts",
                        new AgentSystemException(e.getMessage(), e));
            }

            // Notify modules AFTER configuration is cleared, but handle JWT failures gracefully
            try {
                logDebug("Notifying modules after configuration update");
                notifyModules();
                logDebug("Module notification completed");
            } catch (Exception e) {
                logWarning("Some module notifications failed during deprovisioning: " + e.getMessage());
            }
            
            String resultMessage = deprovisionRequestSuccessful ? 
                "Success - deprovisioned from controller and cleaned up locally" :
                "Success - cleaned up locally (controller deprovision failed)";
            
            logInfo("Finished Deprovisioning : " + resultMessage);
            return "\n" + resultMessage;
            
        } finally {
            provisioningLock.unlock();
        }
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
     * sends IOFog GPS configuration when any changes applied
     */
    public void instanceGpsConfigUpdated() {
        logDebug("Start IOFog GPS configuration update");
        try {
            postGpsConfig();
        } catch (Exception e) {
            logError("Error posting updated for GPS config ", e);
        }
        logDebug("Finished IOFog GPS configuration update");
    }

    /**
     * starts Field Agent module
     */
    public void start() {
        logDebug("Start the Field Agent");
        
        // Initialize JWT Manager first if we have the private key
        if (!isNullOrEmpty(Configuration.getIofogUuid()) && !isNullOrEmpty(Configuration.getPrivateKey())) {
            // Try to generate JWT to verify private key is valid
            if (JwtManager.generateJwt() == null) {
                logError("Failed to initialize JWT Manager", new AgentSystemException("Failed to initialize JWT Manager"));
                StatusReporter.setFieldAgentStatus().setControllerStatus(NOT_PROVISIONED);
            } else {
                StatusReporter.setFieldAgentStatus().setControllerStatus(OK);
            }
        } else {
            StatusReporter.setFieldAgentStatus().setControllerStatus(NOT_PROVISIONED);
        }

        // Initialize other components
        microserviceManager = MicroserviceManager.getInstance();
        orchestrator = new Orchestrator();
        sshProxyManager = new SshProxyManager(new SshConnection());
        edgeResourceManager = EdgeResourceManager.getInstance();
        volumeMountManager = VolumeMountManager.getInstance();
        logSessionManager = new LogSessionManager();
        boolean isConnected = ping();
        getFogConfig();
        if (!notProvisioned()) {
            loadRegistries(!isConnected);
            loadVolumeMounts();
            List<Microservice> microservices = loadMicroservices(!isConnected);
            processMicroserviceConfig(microservices);
            // Notify ProcessManager to immediately restart monitoring thread
            // This ensures containers are processed during initialization without waiting
            ProcessManager.getInstance().update();
            loadEdgeResources(!isConnected);
        }

        // Start background threads
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

    private void handleExecSessions(List<Microservice> microservices) {
        LoggingService.logDebug(MODULE_NAME, "Starting handleExecSessions for " + microservices.size() + " microservices");
        logDebug("Start handling exec sessions");

        CompletableFuture<?>[] futures = microservices.stream()
            .map(microservice -> CompletableFuture.runAsync(() -> {
                LoggingService.logDebug(MODULE_NAME, "Processing exec session for microservice: " + microservice.getMicroserviceUuid() + ", exec enabled: " + microservice.isExecEnabled());
                if (!microservice.isExecEnabled()) {
                    LoggingService.logDebug(MODULE_NAME, "Exec is disabled for microservice: " + microservice.getMicroserviceUuid());
                    // Handle disabled exec sessions
                    String existingExecId = getCurrentExecSessionId(microservice.getMicroserviceUuid());
                    if (existingExecId != null) {
                        LoggingService.logDebug(MODULE_NAME, "Found existing exec session to cleanup: " + existingExecId);
                        try {
                            // Kill exec session asynchronously
                            CompletableFuture.runAsync(() -> {
                                try {
                                    LoggingService.logDebug(MODULE_NAME, "Killing exec session: " + existingExecId);
                                    ProcessManager.getInstance().killExecSession(existingExecId);
                                    LoggingService.logDebug(MODULE_NAME, "Successfully killed exec session: " + existingExecId);
                                } catch (Exception e) {
                                    logError("Failed to kill exec session for microservice: " + microservice.getMicroserviceUuid(), e);
                                }
                            });

                            // Handle WebSocket cleanup asynchronously
                            CompletableFuture.runAsync(() -> {
                                LoggingService.logDebug(MODULE_NAME, "Cleaning up WebSocket for microservice: " + microservice.getMicroserviceUuid());
                                ExecSessionWebSocketHandler wsHandler = activeWebSockets.remove(microservice.getMicroserviceUuid());
                                if (wsHandler != null) {
                                    LoggingService.logDebug(MODULE_NAME, "Found active WebSocket handler, disconnecting");
                                    wsHandler.disconnect();
                                    LoggingService.logDebug(MODULE_NAME, "Successfully disconnected WebSocket handler");
                                } else {
                                    LoggingService.logDebug(MODULE_NAME, "No active WebSocket handler found to disconnect");
                                }
                                activeExecSessions.remove(microservice.getMicroserviceUuid());
                                execCallbacks.remove(microservice.getMicroserviceUuid());
                                LoggingService.logDebug(MODULE_NAME, "Cleaned up exec session and callback maps");
                            });
                        } catch (Exception e) {
                            logError("Failed to handle exec session cleanup for microservice: " + microservice.getMicroserviceUuid(), e);
                        }
                    } else {
                        LoggingService.logDebug(MODULE_NAME, "No existing exec session found to cleanup for microservice: " + microservice.getMicroserviceUuid());
                    }
                } else {
                    LoggingService.logDebug(MODULE_NAME, "Exec is enabled for microservice: " + microservice.getMicroserviceUuid());
                    // Handle enabled exec sessions
                    try {
                        String execId = getCurrentExecSessionId(microservice.getMicroserviceUuid());
                        
                        if (execId != null) {
                            LoggingService.logDebug(MODULE_NAME, "Found existing exec session: " + execId);
                            // Check if existing session is still valid
                            ExecSessionStatus status = ProcessManager.getInstance().getExecSessionStatus(execId);
                            LoggingService.logDebug(MODULE_NAME, "Exec session status: " + (status != null ? "running=" + status.isRunning() : "null"));
                            if (status == null || !status.isRunning()) {
                                LoggingService.logDebug(MODULE_NAME, "Existing exec session is not running, creating new session");
                                // Only create new session if current one is not running
                                CompletableFuture.runAsync(() -> {
                                    try {
                                        // Create new exec session with fallback shell command
                                        String[] command = {"sh", "-c", "clear; (bash || ash || sh)"};
                                        LoggingService.logDebug(MODULE_NAME, "Creating new exec session with command: " + String.join(" ", command));
                                        ExecSessionCallback callback = new ExecSessionCallback(
                                            microservice.getMicroserviceUuid(),
                                            execId
                                        );
                                        ProcessManager.getInstance().createExecSession(
                                            microservice.getMicroserviceUuid(), command, callback)
                                        .thenAccept(newExecId -> {
                                            LoggingService.logDebug(MODULE_NAME, "Created new exec session: " + newExecId);
                                            // Store the new session info
                                            activeExecSessions.put(microservice.getMicroserviceUuid(), newExecId);
                                            execCallbacks.put(microservice.getMicroserviceUuid(), callback);
                                            LoggingService.logDebug(MODULE_NAME, "Stored new session info in maps");

                                            // Set up callback handlers
                                            handleExecSessionCallback(microservice.getMicroserviceUuid(), callback);

                                            // Create and connect WebSocket handler
                                            LoggingService.logDebug(MODULE_NAME, "Creating and connecting WebSocket handler");
                                            ExecSessionWebSocketHandler wsHandler = ExecSessionWebSocketHandler.getInstance(microservice.getMicroserviceUuid());
                                            LoggingService.logDebug(MODULE_NAME, "Got WebSocket handler instance, checking if already exists in activeWebSockets");
                                            if (activeWebSockets.containsKey(microservice.getMicroserviceUuid())) {
                                                LoggingService.logDebug(MODULE_NAME, "Found existing WebSocket handler, cleaning up before creating new one");
                                                ExecSessionWebSocketHandler existingHandler = activeWebSockets.get(microservice.getMicroserviceUuid());
                                                existingHandler.disconnect();
                                                activeWebSockets.remove(microservice.getMicroserviceUuid());
                                            }
                                            LoggingService.logDebug(MODULE_NAME, "Connecting new WebSocket handler");
                                            wsHandler.connect();
                                            activeWebSockets.put(microservice.getMicroserviceUuid(), wsHandler);
                                            LoggingService.logDebug(MODULE_NAME, "Successfully created and connected WebSocket handler");
                                        })
                                        .exceptionally(e -> {
                                            logError("Failed to create new exec session for microservice: " + microservice.getMicroserviceUuid(), new AgentSystemException(e.getMessage(), e));
                                            return null;
                                        });
                                    } catch (Exception e) {
                                        logError("Failed to create new exec session for microservice: " + microservice.getMicroserviceUuid(), e);
                                    }
                                });
                            } else {
                                LoggingService.logDebug(MODULE_NAME, "Existing exec session is still running: " + execId);
                            }
                        } else {
                            LoggingService.logDebug(MODULE_NAME, "No existing exec session found, creating new one");
                            // No existing session, create new one
                            CompletableFuture.runAsync(() -> {
                                try {
                                    // Create new exec session with fallback shell command
                                    String[] command = {"sh", "-c", "clear; (bash || ash || sh)"};
                                    LoggingService.logDebug(MODULE_NAME, "Creating new exec session with command: " + String.join(" ", command));
                                    ExecSessionCallback callback = new ExecSessionCallback(
                                        microservice.getMicroserviceUuid(),
                                        execId
                                    );
                                    ProcessManager.getInstance().createExecSession(
                                        microservice.getMicroserviceUuid(), command, callback)
                                    .thenAccept(newExecId -> {
                                        LoggingService.logDebug(MODULE_NAME, "Created new exec session: " + newExecId);
                                        // Store the new session info
                                        activeExecSessions.put(microservice.getMicroserviceUuid(), newExecId);
                                        execCallbacks.put(microservice.getMicroserviceUuid(), callback);
                                        LoggingService.logDebug(MODULE_NAME, "Stored new session info in maps");

                                        // Set up callback handlers
                                        handleExecSessionCallback(microservice.getMicroserviceUuid(), callback);

                                        // Create and connect WebSocket handler
                                        LoggingService.logDebug(MODULE_NAME, "Creating and connecting WebSocket handler");
                                        ExecSessionWebSocketHandler wsHandler = ExecSessionWebSocketHandler.getInstance(microservice.getMicroserviceUuid());
                                        LoggingService.logDebug(MODULE_NAME, "Got WebSocket handler instance, checking if already exists in activeWebSockets");
                                        if (activeWebSockets.containsKey(microservice.getMicroserviceUuid())) {
                                            LoggingService.logDebug(MODULE_NAME, "Found existing WebSocket handler, cleaning up before creating new one");
                                            ExecSessionWebSocketHandler existingHandler = activeWebSockets.get(microservice.getMicroserviceUuid());
                                            existingHandler.disconnect();
                                            activeWebSockets.remove(microservice.getMicroserviceUuid());
                                        }
                                        LoggingService.logDebug(MODULE_NAME, "Connecting new WebSocket handler");
                                        wsHandler.connect();
                                        activeWebSockets.put(microservice.getMicroserviceUuid(), wsHandler);
                                        LoggingService.logDebug(MODULE_NAME, "Successfully created and connected WebSocket handler");
                                    })
                                    .exceptionally(e -> {
                                        logError("Failed to create new exec session for microservice: " + microservice.getMicroserviceUuid(), new AgentSystemException(e.getMessage(), e));
                                        return null;
                                    });
                                } catch (Exception e) {
                                    logError("Failed to create new exec session for microservice: " + microservice.getMicroserviceUuid(), e);
                                }
                            });
                        }
                    } catch (Exception e) {
                        logError("Failed to handle exec session for microservice: " + microservice.getMicroserviceUuid(), e);
                    }
                }
            }))
            .toArray(CompletableFuture[]::new);

        // Wait for all async operations to complete
        CompletableFuture.allOf(futures)
            .exceptionally(throwable -> {
                logError("Error during async exec session handling", new AgentSystemException(throwable.getMessage(), throwable));
                return null;
            });
        LoggingService.logDebug(MODULE_NAME, "Completed handleExecSessions processing");
    }

    private void handleExecSessionCallback(String microserviceUuid, ExecSessionCallback callback) {
        LoggingService.logDebug(MODULE_NAME, "Setting up exec session callback for microservice: " + microserviceUuid);
        try {
            // Add callback to active callbacks map
            activeExecCallbacks.put(microserviceUuid, callback);
            LoggingService.logDebug(MODULE_NAME, "Added callback to activeExecCallbacks map");

            // Set up input handler
            callback.setOnInputHandler(data -> {
                LoggingService.logDebug(MODULE_NAME, "Input handler called with data length: " + data.length);
                handleExecSessionOutput(microserviceUuid, (byte) 0, data);
            });

            // Set up output handler
            callback.setOnOutputHandler(data -> {
                LoggingService.logDebug(MODULE_NAME, "Output handler called with data length: " + data.length);
                handleExecSessionOutput(microserviceUuid, (byte) 1, data);
            });

            // Set up error handler
            callback.setOnErrorHandler(data -> {
                LoggingService.logDebug(MODULE_NAME, "Error handler called with data length: " + data.length);
                handleExecSessionOutput(microserviceUuid, (byte) 2, data);
            });

            // Set up close handler
            callback.setOnCloseHandler(() -> {
                LoggingService.logDebug(MODULE_NAME, "Close handler called");
                cleanupExecSession(microserviceUuid);
            });

            LoggingService.logDebug(MODULE_NAME, "Successfully set up exec session callback handlers");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error setting up exec session callback", e);
        }
    }

    private void cleanupExecSession(String microserviceUuid) {
        try {
            LoggingService.logInfo(MODULE_NAME, "Cleaning up exec session for microservice: " + microserviceUuid);
            
            // Remove from active sessions
            activeExecSessions.remove(microserviceUuid);
            
            // Cleanup callback
            ExecSessionCallback callback = activeExecCallbacks.remove(microserviceUuid);
            if (callback != null) {
                callback.close();
            }
            
            // Cleanup WebSocket if no other sessions
            if (!activeExecSessions.containsKey(microserviceUuid)) {
                ExecSessionWebSocketHandler handler = activeWebSockets.remove(microserviceUuid);
                if (handler != null) {
                    handler.disconnect();
                }
            }
            
            LoggingService.logInfo(MODULE_NAME, "Exec session cleanup completed");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error cleaning up exec session", e);
        }
    }

    private void handleExecSessionOutput(String microserviceUuid, byte outputType, byte[] output) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Handling exec session output for microservice: " + microserviceUuid + 
                ", type: " + outputType + ", length: " + output.length);
            
            ExecSessionWebSocketHandler handler = activeWebSockets.get(microserviceUuid);
            if (handler == null) {
                LoggingService.logWarning(MODULE_NAME, "No active WebSocket handler found for microservice: " + microserviceUuid);
                return;
            }
            
            if (!handler.isConnected()) {
                LoggingService.logWarning(MODULE_NAME, "WebSocket handler not connected for microservice: " + microserviceUuid);
                return;
            }
            
            handler.sendMessage(outputType, output);
            LoggingService.logDebug(MODULE_NAME, "Successfully sent output to WebSocket");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error handling exec session output", e);
        }
    }

    public Map<String, String> getActiveExecSessions() {
        return Collections.unmodifiableMap(activeExecSessions);
    }

    public Map<String, ExecSessionCallback> getExecCallbacks() {
        return Collections.unmodifiableMap(execCallbacks);
    }

    public Map<String, ExecSessionCallback> getActiveExecCallbacks() {
        return Collections.unmodifiableMap(activeExecCallbacks);
    }

    public Map<String, ExecSessionWebSocketHandler> getActiveWebSockets() {
        return Collections.unmodifiableMap(activeWebSockets);
    }

    private String getCurrentExecSessionId(String microserviceUuid) {
        return activeExecSessions.get(microserviceUuid);
    }

    public void handleExecSessionClose(String microserviceUuid, String execId) {
        LoggingService.logInfo(MODULE_NAME, "Handling exec session close for microservice: " + microserviceUuid + 
            ", execId: " + execId);
        
        try {
            // Kill the exec session
            LoggingService.logDebug(MODULE_NAME, "Killing exec session: " + execId);
            ProcessManager.getInstance().killExecSession(execId);
            LoggingService.logDebug(MODULE_NAME, "Successfully killed exec session: " + execId);
            
            // Cleanup session tracking
            if (activeExecSessions.containsKey(microserviceUuid) && 
                activeExecSessions.get(microserviceUuid).equals(execId)) {
                LoggingService.logDebug(MODULE_NAME, "Removing exec session from tracking");
                activeExecSessions.remove(microserviceUuid);
            }
            
            // Cleanup callback
            ExecSessionCallback callback = activeExecCallbacks.remove(microserviceUuid);
            if (callback != null) {
                LoggingService.logDebug(MODULE_NAME, "Cleaning up callback");
                callback.close();
            }
            
            // Cleanup WebSocket if no other sessions
            if (!activeExecSessions.containsKey(microserviceUuid)) {
                LoggingService.logDebug(MODULE_NAME, "No other active sessions, cleaning up WebSocket");
                ExecSessionWebSocketHandler handler = activeWebSockets.remove(microserviceUuid);
                if (handler != null) {
                    handler.disconnect();
                }
            } else {
                LoggingService.logDebug(MODULE_NAME, "Other active sessions exist, keeping WebSocket connection");
            }
            
            LoggingService.logInfo(MODULE_NAME, "Exec session close handling completed for microservice: " + microserviceUuid);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error handling exec session close", e);
        }
    }

    /**
     * Fetches log sessions from controller
     * @return List of LogSession objects
     * @throws Exception if fetch fails
     */
    private List<LogSession> fetchLogSessions() throws Exception {
        logDebug("Start fetching log sessions from controller");
        List<LogSession> sessions = new ArrayList<>();
        
        if (notProvisioned() || !isControllerConnected(false)) {
            logDebug("Not provisioned or not connected, returning empty list");
            return sessions;
        }

        // Check thread interruption before making request
        if (Thread.currentThread().isInterrupted()) {
            logWarning("Thread interrupted before making log sessions request");
            throw new InterruptedException("Thread interrupted before request");
        }

        try {
            logDebug("Making request to controller for log sessions");
            JsonObject response = orchestrator.request("logs/sessions", RequestType.GET, null, null);
            logDebug("Received response from controller, parsing log sessions");
            if (response != null && response.containsKey("logSessions")) {
                JsonArray logSessionsArray = response.getJsonArray("logSessions");
                if (logSessionsArray != null) {
                    for (int i = 0; i < logSessionsArray.size(); i++) {
                        JsonObject sessionJson = logSessionsArray.getJsonObject(i);
                        LogSession session = parseLogSession(sessionJson);
                        if (session != null) {
                            sessions.add(session);
                        }
                    }
                }
            }
            logDebug("Fetched " + sessions.size() + " log sessions from controller");
        } catch (CertificateException | SSLHandshakeException e) {
            verificationFailed(e);
            logError("Unable to get log sessions due to broken certificate",
                    new AgentSystemException(e.getMessage(), e));
            throw e;
        } catch (Exception e) {
            logError("Unable to get log sessions", new AgentSystemException(e.getMessage(), e));
            throw e;
        }
        
        logDebug("Finished fetching log sessions");
        return sessions;
    }

    /**
     * Parses a JSON object into a LogSession
     */
    private LogSession parseLogSession(JsonObject jsonObj) {
        try {
            String sessionId = jsonObj.getString("sessionId");
            String microserviceUuid = jsonObj.containsKey("microserviceUuid") && !jsonObj.isNull("microserviceUuid") 
                ? jsonObj.getString("microserviceUuid") : null;
            String iofogUuid = jsonObj.containsKey("iofogUuid") && !jsonObj.isNull("iofogUuid")
                ? jsonObj.getString("iofogUuid") : null;
            String status = jsonObj.containsKey("status") ? jsonObj.getString("status") : "PENDING";
            boolean agentConnected = jsonObj.containsKey("agentConnected") ? jsonObj.getBoolean("agentConnected") : false;

            // Parse tailConfig
            Map<String, Object> tailConfig = new HashMap<>();
            if (jsonObj.containsKey("tailConfig") && !jsonObj.isNull("tailConfig")) {
                JsonObject tailConfigJson = jsonObj.getJsonObject("tailConfig");
                if (tailConfigJson != null) {
                    // Parse tailConfig fields
                    if (tailConfigJson.containsKey("lines")) {
                        tailConfig.put("lines", tailConfigJson.getInt("lines"));
                    }
                    if (tailConfigJson.containsKey("follow")) {
                        tailConfig.put("follow", tailConfigJson.getBoolean("follow"));
                    }
                    if (tailConfigJson.containsKey("since") && !tailConfigJson.isNull("since")) {
                        tailConfig.put("since", tailConfigJson.getString("since"));
                    }
                    if (tailConfigJson.containsKey("until") && !tailConfigJson.isNull("until")) {
                        tailConfig.put("until", tailConfigJson.getString("until"));
                    }
                }
            }

            LogSession session = new LogSession(sessionId, microserviceUuid, iofogUuid, tailConfig, status, agentConnected);
            return session;
        } catch (Exception e) {
            logError("Error parsing log session from JSON", new AgentSystemException(e.getMessage(), e));
            return null;
        }
    }

    /**
     * Handles log sessions changes
     */
    private void handleLogSessions() {
        logDebug("Start handling log sessions");
        
        // Check if thread is already interrupted
        if (Thread.currentThread().isInterrupted()) {
            logWarning("Thread already interrupted before handling log sessions");
            return;
        }
        
        try {
            List<LogSession> sessions = fetchLogSessions();
            if (logSessionManager != null) {
                logSessionManager.handleLogSessions(sessions);
            } else {
                logError("LogSessionManager is not initialized", new AgentSystemException("LogSessionManager is null", null));
            }
        } catch (AgentSystemException e) {
            // Check if it's an interruption (might be transient)
            if (e.getMessage() != null && e.getMessage().contains("Request interrupted")) {
                logWarning("Log session fetch was interrupted (may be transient): " + e.getMessage());
                // Don't reset changes flag for interruptions - allow retry on next change detection
            } else {
                logError("Unable to handle log sessions", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logWarning("Thread interrupted while handling log sessions: " + e.getMessage());
            // Don't reset changes flag for interruptions - allow retry on next change detection
        } catch (Exception e) {
            logError("Unable to handle log sessions", e);
        }
        logDebug("Finished handling log sessions");
    }
}