package co.sena.academic.adapter.in.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "academic.events.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class AcademicEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcademicEventConsumer.class);
    private static final String CONSUMER = "academic-management-r1";

    private final JdbcTemplate jdbcTemplate;

    public AcademicEventConsumer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @RabbitListener(
            queues = "${academic.events.consumer.queue:academic-events.r1.consumer}",
            containerFactory = "academicEventsListenerContainerFactory")
    @Transactional
    public void consume(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("event_id/message_id is required");
        }

        UUID eventId = UUID.fromString(messageId);
        Object eventTypeHeader = message.getMessageProperties().getHeaders().get("event_type");
        String eventType = eventTypeHeader == null ? "unknown" : eventTypeHeader.toString();
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        int inserted = jdbcTemplate.update("""
                INSERT INTO academic_management.processed_event
                    (event_id, event_type, consumer_name, payload)
                VALUES (?, ?, ?, CAST(? AS jsonb))
                ON CONFLICT (consumer_name, event_id) DO NOTHING
                """, eventId, eventType, CONSUMER, payload);

        if (inserted == 1) {
            LOGGER.info("academic_event_processed event_id={} event_type={}", eventId, eventType);
        } else {
            LOGGER.info("academic_event_duplicate_ignored event_id={} event_type={}", eventId, eventType);
        }
    }
}
