package org.eclipse.iofog.security_manager;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class SecurityStatus {
    private String securityViolationInfo = "";
    private Status status = Status.OK;

    public enum Status {
        OK,
        QUARANTINE
    }

    public void setSecurityViolationInfo(String securityViolationInfo) {
        this.securityViolationInfo = securityViolationInfo;
    }

    public Status getStatus() {
        return this.status;
    }

    public String getSecurityViolationInfo() {
        return this.securityViolationInfo;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
