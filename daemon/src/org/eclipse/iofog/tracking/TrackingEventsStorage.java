package org.eclipse.iofog.tracking;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TrackingEventsStorage {
    private static TrackingEventsStorage instance = null;
    public static TrackingEventsStorage getInstance() {
        if (instance == null) {
            synchronized (TrackingEventsStorage.class) {
                if (instance == null)
                    instance = new TrackingEventsStorage();
            }
        }
        return instance;
    }

    private static List<TrackingEvent> events = new CopyOnWriteArrayList<>();

    protected synchronized void pushEvent(TrackingEvent event) {
        events.add(event);
    }

    protected synchronized List<TrackingEvent> popAllEvents() {
        List<TrackingEvent> res = events.subList(0, events.size() - 1);
        events.clear();
        return events;
    }
}
