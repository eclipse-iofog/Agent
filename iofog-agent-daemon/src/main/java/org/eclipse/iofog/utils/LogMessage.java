package org.eclipse.iofog.utils;

/**
 * Message class for log session WebSocket communication
 */
public class LogMessage {
    private byte type;  // 6: LOG_LINE, 7: LOG_START, 8: LOG_STOP, 9: LOG_ERROR
    private byte[] data;
    private String sessionId;
    private String microserviceUuid;  // For microservice logs
    private String iofogUuid;         // For fog logs
    private long timestamp;

    // Default constructor required by MessagePack
    public LogMessage() {}

    public LogMessage(byte type, byte[] data, String sessionId, String microserviceUuid, String iofogUuid) {
        this.type = type;
        this.data = data;
        this.sessionId = sessionId;
        this.microserviceUuid = microserviceUuid;
        this.iofogUuid = iofogUuid;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public byte getType() { return type; }
    public void setType(byte type) { this.type = type; }
    
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getMicroserviceUuid() { return microserviceUuid; }
    public void setMicroserviceUuid(String microserviceUuid) { this.microserviceUuid = microserviceUuid; }
    
    public String getIofogUuid() { return iofogUuid; }
    public void setIofogUuid(String iofogUuid) { this.iofogUuid = iofogUuid; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

