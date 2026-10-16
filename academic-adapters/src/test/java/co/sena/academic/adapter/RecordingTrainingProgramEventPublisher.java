package co.sena.academic.adapter;

import co.sena.academic.application.port.out.TrainingProgramEventPublisherPort;

import java.util.ArrayList;
import java.util.List;

public class RecordingTrainingProgramEventPublisher implements TrainingProgramEventPublisherPort {

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
