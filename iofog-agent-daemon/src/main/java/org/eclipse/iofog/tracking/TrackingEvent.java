package org.eclipse.iofog.tracking;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;

public class TrackingEvent {
    private String uuid;
    private String sourceType;
    private Long timestamp;
    private TrackingEventType type;
    private JsonStructure data;
    private final String MODULE_NAME = "TrackingEvent";

    public TrackingEvent(String uuid, Long timestamp, TrackingEventType type, JsonStructure data) throws AgentSystemException {
        if (uuid != null && timestamp != null && type != null && data !=null){
            this.uuid = uuid;
            this.timestamp = timestamp;
            this.sourceType = "agent";
            this.type = type;
            this.data = data;
        } else {
            LoggingService.logError(MODULE_NAME, "Error creating TrackingEvent object",
                    new AgentSystemException("Error creating TrackingEvent object : arguments cannot be null"));
            throw new AgentSystemException("Error creating TrackingEvent object : arguments cannot be null");
        }

    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public TrackingEventType getType() {
        return type;
    }

    public void setType(TrackingEventType type) {
        this.type = type;
    }

    public JsonStructure getData() {
        return data;
    }

    public void setData(JsonStructure data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return toJsonObject().toString();
    }

    public JsonObject toJsonObject() {
        LoggingService.logInfo(MODULE_NAME, "Getting JsonObject of TrackingEvent");
        return Json.createObjectBuilder()
                .add("uuid", uuid)
                .add("timestamp", timestamp)
                .add("sourceType", sourceType)
                .add("type", type.getName())
                .add("data", data)
                .build();
    }
}
