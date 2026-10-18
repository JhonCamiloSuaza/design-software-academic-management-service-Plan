package co.sena.academic.application.port.out;

import co.sena.academic.domain.model.Ficha;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface FichaEventPublisherPort {

    void publish(EventEnvelope event);

    record EventEnvelope(UUID event_id,
                         String event_type,
                         int version,
                         Instant timestamp,
                         String source_service,
                         FichaPayload payload,
                         String correlation_id) {
    }

    record FichaPayload(UUID ficha_id,
                        String ficha_number,
                        UUID program_id,
                        int program_version,
                        UUID training_center_id,
                        String status,
                        LocalDate start_date,
                        String training_shift,
                        String training_modality,
                        int max_capacity,
                        UUID opened_by,
                        Instant opened_at,
                        String close_reason,
                        LocalDate actual_end_date,
                        UUID closed_by,
                        Instant closed_at) {

        public static FichaPayload opened(Ficha ficha, UUID actorId) {
            return new FichaPayload(
                    ficha.getId(),
                    ficha.getFichaNumber(),
                    ficha.getProgramId(),
                    ficha.getProgramVersion(),
                    ficha.getTrainingCenterId(),
                    ficha.getStatus(),
                    ficha.getStartDate(),
                    ficha.getTrainingShift(),
                    ficha.getTrainingModality(),
                    ficha.getMaxCapacity(),
                    actorId,
                    ficha.getCreatedAt(),
                    null,
                    null,
                    null,
                    null);
        }

        public static FichaPayload closed(Ficha ficha, UUID actorId) {
            return new FichaPayload(
                    ficha.getId(),
                    ficha.getFichaNumber(),
                    ficha.getProgramId(),
                    ficha.getProgramVersion(),
                    ficha.getTrainingCenterId(),
                    ficha.getStatus(),
                    ficha.getStartDate(),
                    ficha.getTrainingShift(),
                    ficha.getTrainingModality(),
                    ficha.getMaxCapacity(),
                    null,
                    null,
                    ficha.getStatus(),
                    ficha.getActualEndDate(),
                    actorId,
                    ficha.getUpdatedAt());
        }
    }
}
