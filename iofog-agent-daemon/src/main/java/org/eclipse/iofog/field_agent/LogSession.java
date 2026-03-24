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

import java.util.Map;

/**
 * Represents a log session configuration from the controller
 * 
 * @author Datasance
 */
public class LogSession {
    private String sessionId;
    private String microserviceUuid;  // For microservice logs: UUID of the microservice that needs logging. Null for fog logs.
    private String iofogUuid;         // For fog/agent logs: UUID of the agent itself. Null for microservice logs.
    private Map<String, Object> tailConfig;  // Configuration for log tailing (lines, follow, since, until)
    private String status;            // PENDING, ACTIVE
    private boolean agentConnected;   // Whether agent has connected to the WebSocket

    public LogSession() {
    }

    public LogSession(String sessionId, String microserviceUuid, String iofogUuid, 
                     Map<String, Object> tailConfig, String status, boolean agentConnected) {
        this.sessionId = sessionId;
        this.microserviceUuid = microserviceUuid;
        this.iofogUuid = iofogUuid;
        this.tailConfig = tailConfig;
        this.status = status;
        this.agentConnected = agentConnected;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMicroserviceUuid() {
        return microserviceUuid;
    }

    public void setMicroserviceUuid(String microserviceUuid) {
        this.microserviceUuid = microserviceUuid;
    }

    public String getIofogUuid() {
        return iofogUuid;
    }

    public void setIofogUuid(String iofogUuid) {
        this.iofogUuid = iofogUuid;
    }

    public Map<String, Object> getTailConfig() {
        return tailConfig;
    }

    public void setTailConfig(Map<String, Object> tailConfig) {
        this.tailConfig = tailConfig;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAgentConnected() {
        return agentConnected;
    }

    public void setAgentConnected(boolean agentConnected) {
        this.agentConnected = agentConnected;
    }

    /**
     * Check if this is a microservice log session
     */
    public boolean isMicroserviceLog() {
        return microserviceUuid != null && !microserviceUuid.isEmpty();
    }

    /**
     * Check if this is a fog/agent log session
     */
    public boolean isFogLog() {
        return iofogUuid != null && !iofogUuid.isEmpty();
    }
}

