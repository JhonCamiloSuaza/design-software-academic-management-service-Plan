package co.sena.academic.adapter.out.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

public class AcademicOutboxWriter {

    private static final String TOPIC = "academic-events";

    private final AcademicOutboxJpaRepository repository;
    private final ObjectMapper objectMapper;

    public AcademicOutboxWriter(AcademicOutboxJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper.copy()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public void enqueue(UUID eventId, String eventType, Object envelope, Instant createdAt) {
        try {
            repository.save(new AcademicOutboxJpaEntity(
                    UUID.randomUUID(),
                    eventId,
                    eventType,
                    TOPIC,
                    eventType,
                    objectMapper.writeValueAsString(envelope),
                    createdAt));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize academic domain event " + eventType, ex);
        }
    }
}
