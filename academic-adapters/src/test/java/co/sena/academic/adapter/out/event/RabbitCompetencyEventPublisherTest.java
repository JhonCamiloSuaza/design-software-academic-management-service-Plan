package co.sena.academic.adapter.out.event;

import co.sena.academic.application.port.out.CompetencyEventPublisherPort;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitCompetencyEventPublisherTest {

    @Test
    void publishesToAcademicEventsUsingCanonicalRoutingKey() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        TopicExchange exchange = new TopicExchange("academic-events", true, false);
        RabbitCompetencyEventPublisher publisher = new RabbitCompetencyEventPublisher(
                rabbitTemplate,
                exchange,
                "academic.competency.updated");

        UUID eventId = UUID.randomUUID();
        UUID competencyId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.now();

        var event = new CompetencyEventPublisherPort.EventEnvelope(
                eventId,
                "academic.competency.updated",
                1,
                now,
                "academic-management-service",
                new CompetencyEventPublisherPort.CompetencyPayload(
                        competencyId,
                        "220501001",
                        programId,
                        "UPDATED",
                        actorId,
                        now),
                null);

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(
                eq("academic-events"),
                eq("academic.competency.updated"),
                same(event),
                any(MessagePostProcessor.class));
    }
}
