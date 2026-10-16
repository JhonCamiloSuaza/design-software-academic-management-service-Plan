package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.TrainingProgram;

import java.util.UUID;

/** E1 - POST /training-programs. */
public interface CreateTrainingProgramUseCase {

    TrainingProgram create(Command command);

    record Command(
            UUID knowledgeNetworkId,
            String programCode,
            String name,
            String trainingLevel,
            int totalHours,
            Boolean isActive,
            UUID actorId) {
    }
}
