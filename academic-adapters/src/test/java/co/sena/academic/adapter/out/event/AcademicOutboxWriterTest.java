package co.sena.academic.adapter.out.event;

import co.sena.academic.application.port.out.FichaEventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AcademicOutboxWriterTest {

    @Test
    void writesCanonicalEnvelopeWithoutDeprecatedFields() throws Exception {
        AcademicOutboxJpaRepository repository = mock(AcademicOutboxJpaRepository.class);
        AcademicOutboxWriter writer = new AcademicOutboxWriter(repository, new ObjectMapper().findAndRegisterModules());
        UUID eventId = UUID.randomUUID();

        var envelope = new FichaEventPublisherPort.EventEnvelope(
                eventId,
                "academic.ficha.closed",
                1,
                Instant.parse("2026-12-20T12:00:00Z"),
                "academic-management-service",
                new FichaEventPublisherPort.FichaPayload(
                        UUID.randomUUID(),
                        "2874521",
                        UUID.randomUUID(),
                        1,
                        UUID.randomUUID(),
                        "CANCELLED",
                        LocalDate.of(2026, 2, 1),
                        "DAY",
                        "IN_PERSON",
                        30,
                        null,
                        null,
                        "CANCELLED",
                        LocalDate.of(2026, 12, 20),
                        UUID.randomUUID(),
                        Instant.parse("2026-12-20T12:00:00Z")),
                null);

        writer.enqueue(eventId, "academic.ficha.closed", envelope, envelope.timestamp());

        var captor = forClass(AcademicOutboxJpaEntity.class);
        verify(repository).save(captor.capture());
        AcademicOutboxJpaEntity saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo("academic-events");
        assertThat(saved.getRoutingKey()).isEqualTo("academic.ficha.closed");
        assertThat(saved.getPayload()).contains(
                "\"event_id\"",
                "\"event_type\":\"academic.ficha.closed\"",
                "\"version\":1",
                "\"timestamp\"",
                "\"source_service\":\"academic-management-service\"",
                "\"payload\"");
        assertThat(saved.getPayload()).doesNotContain("schema_version", "occurred_at", "source_port", "\"data\"");
    }
}
