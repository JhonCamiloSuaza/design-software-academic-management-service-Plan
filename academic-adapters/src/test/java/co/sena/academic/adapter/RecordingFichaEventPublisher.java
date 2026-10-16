package co.sena.academic.adapter;

import co.sena.academic.application.port.out.FichaEventPublisherPort;

import java.util.ArrayList;
import java.util.List;

/** Explicit in-memory test double for the academic.ficha.opened publisher. */
public class RecordingFichaEventPublisher implements FichaEventPublisherPort {

    private final List<EventEnvelope> events = new ArrayList<>();

    @Override
    public synchronized void publish(EventEnvelope event) {
        events.add(event);
    }

    public synchronized List<EventEnvelope> events() {
        return List.copyOf(events);
    }

    public synchronized void clear() {
        events.clear();
    }
}
