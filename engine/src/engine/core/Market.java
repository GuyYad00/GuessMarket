package engine.core;

import engine.exception.EngineOperationException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The whole loaded system: all events, keyed by their unique id,
 * preserving the order they appeared in the XML file.
 */
public class Market implements Serializable {

    private final Map<Integer, Event> events = new LinkedHashMap<>();

    public void addEvent(Event event) {
        events.put(event.getId(), event);
    }

    public List<Event> getAllEvents() {
        return new ArrayList<>(events.values());
    }

    public List<Event> getActiveEvents() {
        List<Event> active = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.isActive()) {
                active.add(event);
            }
        }
        return active;
    }

    public Event getEventById(int id) {
        Event event = events.get(id);
        if (event == null) {
            throw new EngineOperationException("There is no event with id " + id + " in the system.");
        }
        return event;
    }
}
