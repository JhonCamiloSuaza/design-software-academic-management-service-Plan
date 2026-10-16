package co.sena.academic.adapter.out.event;

import co.sena.academic.application.port.out.FichaEventPublisherPort;

public class OutboxFichaEventPublisher implements FichaEventPublisherPort {

    private final AcademicOutboxWriter outboxWriter;

    public OutboxFichaEventPublisher(AcademicOutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    @Override
    public void publish(EventEnvelope event) {
        outboxWriter.enqueue(event.event_id(), event.event_type(), event, event.timestamp());
    }
}
