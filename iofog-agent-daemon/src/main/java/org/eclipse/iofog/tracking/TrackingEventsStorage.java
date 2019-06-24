package org.eclipse.iofog.tracking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TrackingEventsStorage {

    private List<TrackingEvent> events = new ArrayList<>();

    protected synchronized void pushEvent(TrackingEvent event) {
        events.add(event);
    }

    protected synchronized List<TrackingEvent> popAllEvents() {
        List<TrackingEvent> res = new ArrayList<>(events);
        events.clear();
        return res;
    }
}
