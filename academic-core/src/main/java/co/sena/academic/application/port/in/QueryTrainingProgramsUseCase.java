package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.TrainingProgram;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** E4 - GET /training-programs and GET /training-programs/{id}. */
public interface QueryTrainingProgramsUseCase {

    TrainingProgram getById(UUID id);

    Page list(Query query);

    record Query(
            Optional<UUID> knowledgeNetworkId,
            Optional<String> programCode,
            Optional<String> trainingLevel,
            Optional<Boolean> isActive,
            int page,
            int size,
            Optional<String> sort) {

        public Query(UUID knowledgeNetworkId,
                     String programCode,
                     String trainingLevel,
                     Boolean isActive,
                     int page,
                     int size,
                     String sort) {
            this(
                    Optional.ofNullable(knowledgeNetworkId),
                    Optional.ofNullable(programCode),
                    Optional.ofNullable(trainingLevel),
                    Optional.ofNullable(isActive),
                    page,
                    size,
                    Optional.ofNullable(sort));
        }
    }

    record Page(List<TrainingProgram> content, long totalElements, int page, int size) {
    }
}
