package co.sena.academic.adapter.in.event;

import co.sena.academic.adapter.out.event.ProcessedEventJpaRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** R1 consumer whose durable receipt is the idempotency boundary. */
@Component
@ConditionalOnProperty(name = "academic.events.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class AcademicR1EventConsumer {

    static final String CONSUMER_NAME = "academic-management-r1";
    private final ProcessedEventJpaRepository processedEvents;

    public AcademicR1EventConsumer(ProcessedEventJpaRepository processedEvents) {
        this.processedEvents = processedEvents;
    }

    @RabbitListener(queues = "${academic.events.consumer.queue:academic-events.r1.consumer}",
            containerFactory = "academicListenerContainerFactory")
    @Transactional
    public void consume(Message message) {
        if (Boolean.TRUE.equals(message.getMessageProperties().getHeaders().get("force_failure"))
                || new String(message.getBody(), StandardCharsets.UTF_8).contains("\"force_failure\":true")) {
            throw new IllegalStateException("R1 forced consumer failure");
        }
        String messageId = message.getMessageProperties().getMessageId();
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("Incoming academic event has no message_id");
        }
        String eventType = String.valueOf(message.getMessageProperties().getHeaders()
                .getOrDefault("event_type", "academic.r1.unknown"));
        processedEvents.insertIfAbsent(UUID.fromString(messageId), eventType, CONSUMER_NAME);
    }
}
