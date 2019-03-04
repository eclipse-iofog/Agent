package org.eclipse.iofog.tracking;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;

import static org.eclipse.iofog.utils.Constants.TRACKING_UUID_PATH;

import javax.json.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Tracker implements IOFogModule {
    private final String MODULE_NAME = "Tracker";
    private static Tracker instance = null;
    public static Tracker getInstance() {
        if (instance == null) {
            synchronized (Tracker.class) {
                if (instance == null)
                    instance = new Tracker();
            }
        }
        return instance;
    }

    private String uuid;
    private Timer loggerTimer = null;
    private Timer senderTimer = null;
    private TrackingEventsStorage eventsStorage = new TrackingEventsStorage();
    @Override
    public void start() throws Exception {
        this.uuid = initTrackingUuid();

        loggerTimer = new Timer();
        TimeLoggerTask timeLoggerTask = new TimeLoggerTask();
        loggerTimer.schedule(timeLoggerTask,
                timeLoggerTask.getTimeTrackingTimeoutMin() * 60 * 1000,
                timeLoggerTask.getTimeTrackingTimeoutMin() * 60 * 1000);

        senderTimer = new Timer();
        SenderTask senderTask = new SenderTask();
        senderTimer.schedule(senderTask,
                senderTask.getSendTimeoutMin() * 60 * 1000,
                senderTask.getSendTimeoutMin() * 60 * 1000);
    }

    @Override
    public int getModuleIndex() {
        return Constants.TRACKER;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    private String initTrackingUuid() {
        String uuid;
        Path path = Paths.get(TRACKING_UUID_PATH);
        try {
            if (Files.notExists(path)) {
                return createTrackingUuidFile(path);
            }

            List<String> lines = Files.readAllLines(path);
            if (lines.size() == 0) {
                return createTrackingUuidFile(path);
            }

            uuid = lines.get(0);
            if (uuid.length() < 32) {
                return createTrackingUuidFile(path);
            }
        } catch (IOException e) {
            logError("Error while getting tracking UUID", e);
            uuid = "temp_" + generateRandomString(32);
        }
        return uuid;
    }

    private String createTrackingUuidFile(Path path) throws IOException {
        String uuid;
        uuid = generateRandomString(32);
        Files.write(path, uuid.getBytes(), StandardOpenOption.CREATE);

        return uuid;
    }

    private String generateRandomString(final int size) {

        StringBuffer randString = new StringBuffer();
        final String possible = "2346789bcdfghjkmnpqrtvwxyzBCDFGHJKLMNPQRTVWXYZ";
        Random random = new Random();

        for (int i = 0; i < size; i++) {
            randString.append(possible.charAt(random.nextInt(possible.length())));
        }

        return randString.toString();
    }

    public void handleEvent(TrackingEventType type, String value) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        JsonObject valueObj = null;
        switch (type) {
            case TIME:
                valueObj = jsonObjectBuilder.add("deltaTime", value + " min").build();
                break;
            case ERROR:
                valueObj = jsonObjectBuilder.add("message", value).build();
                break;
            case PROVISION:
                valueObj = jsonObjectBuilder.add("provisionStatus", value).build();
                break;
            case DEPROVISION:
                valueObj = jsonObjectBuilder.add("deprovisionStatus", value).build();
                break;
            default:
                //other events types not handled because should be used with handleEvent(TrackingEventType type, JsonStructure value) method
                throw new IllegalArgumentException("unhandled event type");
        }
        handleEvent(type, valueObj);
    }

    public void handleEvent(TrackingEventType type, JsonStructure value) {
        TrackingEvent event = new TrackingEvent(this.uuid, new Date().getTime(), type, value);
        eventsStorage.pushEvent(event);
    }

    private class TimeLoggerTask extends TimerTask {
        private final int timeTrackingTimeoutMin = 5;
        private int iterations = 0;

        public int getTimeTrackingTimeoutMin() {
            return timeTrackingTimeoutMin;
        }

        @Override
        public boolean cancel() {
            return super.cancel();
        }

        @Override
        public long scheduledExecutionTime() {
            return super.scheduledExecutionTime();
        }

        @Override
        public void run() {
            iterations++;
            handleEvent(TrackingEventType.TIME, Long.toString(iterations * timeTrackingTimeoutMin));
        }
    }

    private class SenderTask extends TimerTask {
        private final int sendTimeoutMin = 5;
        HttpClient httpClient = HttpClients.createDefault();

        public int getSendTimeoutMin() {
            return sendTimeoutMin;
        }

        @Override
        public boolean cancel() {
            return super.cancel();
        }

        @Override
        public long scheduledExecutionTime() {
            return super.scheduledExecutionTime();
        }

        @Override
        public void run() {
            List<TrackingEvent> events = eventsStorage.popAllEvents();
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

            events
                    .forEach(el -> {
                        jsonArrayBuilder.add(el.toJsonObject());
                    });

            JsonObject eventsListObject = Json.createObjectBuilder()
                    .add("events", jsonArrayBuilder.build())
                    .build();

            HttpPost postMethod = null;
            if (StatusReporter.getFieldAgentStatus().getControllerStatus().equals(Constants.ControllerStatus.OK)) {
                //send to controller
                FieldAgent.getInstance().postTracking(eventsListObject);
            } else {
                //send directly
                StringEntity requestEntity = new StringEntity(eventsListObject.toString(), ContentType.APPLICATION_JSON);
                postMethod = new HttpPost("https://analytics.iofog.org/post");
                postMethod.setEntity(requestEntity);

                try {
                    HttpResponse response = httpClient.execute(postMethod);
                } catch (IOException e) {
                    logWarning(e.getMessage());
                }
            }
        }
    }
}
