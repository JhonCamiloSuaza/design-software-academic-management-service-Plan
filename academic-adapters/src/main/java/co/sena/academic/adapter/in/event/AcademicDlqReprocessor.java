package co.sena.academic.adapter.in.event;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Moves one failed R1 message from the DLQ back to its primary route. */
@Component
public class AcademicDlqReprocessor {
    private final RabbitTemplate rabbitTemplate;
    private final String dlq;
    private final String exchange;
    private final String routingKey;

    public AcademicDlqReprocessor(RabbitTemplate rabbitTemplate,
            @Value("${academic.events.consumer.dlq:academic-events.r1.dlq}") String dlq,
            @Value("${academic.events.exchange:academic-events}") String exchange,
            @Value("${academic.events.consumer.routing-key:academic.r1.#}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.dlq = dlq;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public ReprocessResult reprocessNext() {
        Message message = rabbitTemplate.receive(dlq);
        if (message == null) return new ReprocessResult(false, null);
        // `force_failure` exists only to perform the documented chaos test.  An
        // operator's retry removes that test switch while preserving the event id.
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(message.getMessageProperties().getMessageId());
        properties.setContentType(message.getMessageProperties().getContentType());
        Object eventType = message.getMessageProperties().getHeaders().get("event_type");
        if (eventType != null) {
            properties.setHeader("event_type", eventType);
        }
        // Do not carry broker x-death metadata or the test-only failure switch
        // into a new delivery.  The original id remains the traceable identity.
        rabbitTemplate.send(exchange, "academic.r1.reprocess", new Message(message.getBody(), properties));
        return new ReprocessResult(true, message.getMessageProperties().getMessageId());
    }

    public record ReprocessResult(boolean reprocessed, String eventId) { }
}
