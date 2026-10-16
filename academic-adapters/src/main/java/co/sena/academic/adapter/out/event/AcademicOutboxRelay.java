package co.sena.academic.adapter.out.event;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "academic.events.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class AcademicOutboxRelay {

    private static final List<Duration> BACKOFF = List.of(
            Duration.ofSeconds(1), Duration.ofSeconds(4), Duration.ofSeconds(16));

    private final AcademicOutboxJpaRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final int batchSize;

    public AcademicOutboxRelay(AcademicOutboxJpaRepository repository,
                               RabbitTemplate rabbitTemplate,
                               @Value("${academic.events.outbox.batch-size:50}") int batchSize) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${academic.events.outbox.relay-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        Instant now = Instant.now();
        for (AcademicOutboxJpaEntity event : repository.findPublishable(now, PageRequest.of(0, batchSize))) {
            try {
                sendConfirmed(event.getTopic(), event.getRoutingKey(), messageFor(event, null));
                event.markPublished(Instant.now());
            } catch (RuntimeException ex) {
                if (event.getRetryCount() >= BACKOFF.size()) {
                    try {
                        sendConfirmed(event.getTopic(), "academic-events.dlq", messageFor(event, ex.getMessage()));
                        event.markDeadLettered(Instant.now(), ex.getMessage());
                    } catch (RuntimeException dlqFailure) {
                        event.markRetry(Instant.now().plus(BACKOFF.get(BACKOFF.size() - 1)), dlqFailure.getMessage());
                    }
                } else {
                    Duration delay = BACKOFF.get(event.getRetryCount());
                    event.markRetry(Instant.now().plus(delay), ex.getMessage());
                }
            }
        }
    }

    private void sendConfirmed(String exchange, String routingKey, Message message) {
        CorrelationData correlation = new CorrelationData(message.getMessageProperties().getMessageId());
        rabbitTemplate.send(exchange, routingKey, message, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ rejected event: " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ returned unroutable event");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for RabbitMQ confirm", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("RabbitMQ publisher confirm failed", ex);
        }
    }

    private Message messageFor(AcademicOutboxJpaEntity event, String dlqReason) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setMessageId(event.getEventId().toString());
        properties.setHeader("event_type", event.getEventType());
        if (dlqReason != null) {
            properties.setHeader("dlq_reason", dlqReason);
        }
        return new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), properties);
    }
}
