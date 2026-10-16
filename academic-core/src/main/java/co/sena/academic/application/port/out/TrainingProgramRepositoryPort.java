package co.sena.academic.application.port.out;

import co.sena.academic.domain.model.TrainingProgram;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingProgramRepositoryPort {

    TrainingProgram save(TrainingProgram trainingProgram);

    Optional<TrainingProgram> findById(UUID id);

    boolean existsByProgramCode(String programCode);

    boolean existsByProgramCodeAndIdNot(String programCode, UUID excludedId);

    List<TrainingProgram> findAll(
            Optional<UUID> knowledgeNetworkId,
            Optional<String> programCode,
            Optional<String> trainingLevel,
            Optional<Boolean> isActive,
            int page,
            int size,
            Optional<String> sort);

    long count(
            Optional<UUID> knowledgeNetworkId,
            Optional<String> programCode,
            Optional<String> trainingLevel,
            Optional<Boolean> isActive);
}
