package co.sena.academic.application.port.out;

import co.sena.academic.domain.model.Competency;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound port used by HU-ACAD-06 to emit the canonical
 * academic.competency.updated domain event.
 */
public interface CompetencyEventPublisherPort {

    void publish(EventEnvelope event);

    /**
     * Field names mirror the current event envelope contract used by HU-ACAD-13.
     */
    record EventEnvelope(UUID event_id,
                         String event_type,
                         int version,
                         Instant timestamp,
                         String source_service,
                         CompetencyPayload payload,
                         String correlation_id) {
    }

    /**
     * Payload documented for academic.competency.updated.
     * The database field sena_code is exposed as competency_code in the event contract.
     */
    record CompetencyPayload(UUID competency_id,
                             String competency_code,
                             UUID program_id,
                             String change_type,
                             UUID updated_by,
                             Instant updated_at) {
        public static CompetencyPayload from(Competency competency, UUID actorId, String changeType) {
            return new CompetencyPayload(
                    competency.getId(),
                    competency.getSenaCode(),
                    competency.getProgramId(),
                    changeType,
                    actorId,
                    competency.getUpdatedAt());
        }
    }
}
