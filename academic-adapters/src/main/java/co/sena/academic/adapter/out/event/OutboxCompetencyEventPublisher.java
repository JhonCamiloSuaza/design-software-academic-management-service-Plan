package co.sena.academic.adapter.out.event;

import co.sena.academic.application.port.out.CompetencyEventPublisherPort;

public class OutboxCompetencyEventPublisher implements CompetencyEventPublisherPort {

    private final AcademicOutboxWriter outboxWriter;

    public OutboxCompetencyEventPublisher(AcademicOutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    @Override
    public void publish(EventEnvelope event) {
        outboxWriter.enqueue(event.event_id(), event.event_type(), event, event.timestamp());
    }
}
