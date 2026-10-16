package co.sena.academic.adapter.out.event;

import co.sena.academic.application.port.out.TrainingProgramEventPublisherPort;

public class OutboxTrainingProgramEventPublisher implements TrainingProgramEventPublisherPort {

    private final AcademicOutboxWriter outboxWriter;

    public OutboxTrainingProgramEventPublisher(AcademicOutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    @Override
    public void publish(EventEnvelope event) {
        outboxWriter.enqueue(event.event_id(), event.event_type(), event, event.timestamp());
    }
}
