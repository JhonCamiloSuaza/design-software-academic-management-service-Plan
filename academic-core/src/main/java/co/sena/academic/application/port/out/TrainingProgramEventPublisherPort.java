package co.sena.academic.application.port.out;

import co.sena.academic.domain.model.TrainingProgram;

import java.time.Instant;
import java.util.UUID;

public interface TrainingProgramEventPublisherPort {

    void publish(EventEnvelope event);

    record EventEnvelope(UUID event_id,
                         String event_type,
                         int version,
                         Instant timestamp,
                         String source_service,
                         ProgramPayload payload,
                         String correlation_id) {
    }

    record ProgramPayload(UUID program_id,
                          String program_code,
                          String name,
                          String training_level,
                          int total_hours,
                          UUID knowledge_network_id,
                          UUID created_by,
                          Instant created_at) {
        public static ProgramPayload from(TrainingProgram program, UUID actorId) {
            return new ProgramPayload(
                    program.getId(),
                    program.getProgramCode(),
                    program.getName(),
                    program.getTrainingLevel(),
                    program.getTotalHours(),
                    program.getKnowledgeNetworkId(),
                    actorId,
                    program.getCreatedAt());
        }
    }
}
