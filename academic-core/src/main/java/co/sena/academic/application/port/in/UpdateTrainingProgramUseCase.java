package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.TrainingProgram;

import java.util.UUID;

/** E2/E3 - PUT /training-programs/{id}, including row_version validation. */
public interface UpdateTrainingProgramUseCase {

    TrainingProgram update(Command command);

    record Command(
            UUID id,
            UUID knowledgeNetworkId,
            String programCode,
            String name,
            String trainingLevel,
            Integer totalHours,
            Integer version,
            Boolean isActive,
            int rowVersion,
            UUID actorId) {
    }
}