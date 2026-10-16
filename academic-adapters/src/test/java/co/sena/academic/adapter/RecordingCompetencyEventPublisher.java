package co.sena.academic.adapter;

import co.sena.academic.application.port.out.CompetencyEventPublisherPort;

import java.util.ArrayList;
import java.util.List;

/** Test-only publisher used to verify HU-ACAD-06 event generation without a broker. */
public class RecordingCompetencyEventPublisher implements CompetencyEventPublisherPort {

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
