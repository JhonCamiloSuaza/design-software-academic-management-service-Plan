package co.sena.academic.adapter.out.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "academic_management", name = "outbox")
public class AcademicOutboxJpaEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false, length = 120)
    private String topic;

    @Column(name = "routing_key", nullable = false, length = 120)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "dead_lettered_at")
    private Instant deadLetteredAt;

    @Column(name = "last_error")
    private String lastError;

    protected AcademicOutboxJpaEntity() {
    }

    public AcademicOutboxJpaEntity(UUID id,
                                   UUID eventId,
                                   String eventType,
                                   String topic,
                                   String routingKey,
                                   String payload,
                                   Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.topic = topic;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = createdAt;
        this.nextAttemptAt = createdAt;
        this.retryCount = 0;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public String getRoutingKey() { return routingKey; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public int getRetryCount() { return retryCount; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getDeadLetteredAt() { return deadLetteredAt; }
    public String getLastError() { return lastError; }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markRetry(Instant nextAttemptAt, String lastError) {
        this.retryCount += 1;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = truncate(lastError);
    }

    public void markDeadLettered(Instant deadLetteredAt, String lastError) {
        this.deadLetteredAt = deadLetteredAt;
        this.lastError = truncate(lastError);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
