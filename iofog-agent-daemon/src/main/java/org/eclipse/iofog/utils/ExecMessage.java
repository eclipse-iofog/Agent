package org.eclipse.iofog.utils;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;


public class ExecMessage {
    private byte type;  // 0: STDIN, 1: STDOUT, 2: STDERR, 3: CONTROL
    private byte[] data;
    private String microserviceUuid;
    private String execId;
    private long timestamp;

    // Default constructor required by MessagePack
    public ExecMessage() {}

    public ExecMessage(byte type, byte[] data, String microserviceUuid, String execId) {
        this.type = type;
        this.data = data;
        this.microserviceUuid = microserviceUuid;
        this.execId = execId;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public byte getType() { return type; }
    public void setType(byte type) { this.type = type; }
    
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    
    public String getMicroserviceUuid() { return microserviceUuid; }
    public void setMicroserviceUuid(String microserviceUuid) { this.microserviceUuid = microserviceUuid; }
    
    public String getExecId() { return execId; }
    public void setExecId(String execId) { this.execId = execId; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 