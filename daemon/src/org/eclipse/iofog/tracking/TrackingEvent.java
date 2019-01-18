package org.eclipse.iofog.tracking;

import javax.json.Json;
import javax.json.JsonObject;

public class TrackingEvent {
    private String uuid;
    private String sourceType;
    private Long timestamp;
    private TrackingEventType type;
    private String value;

    public TrackingEvent(String uuid, Long timestamp, TrackingEventType type, String value) {
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.sourceType = "agent";
        this.type = type;
        this.value = value;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return toJsonObject().toString();
    }

    public JsonObject toJsonObject() {
        return Json.createObjectBuilder()
                .add("uuid", uuid)
                .add("timestamp", timestamp)
                .add("sourceType", sourceType)
                .add("type", type.getName())
                .add("value", value)
                .build();
    }
}
