package co.sena.academic.application.port.out;

import co.sena.academic.domain.model.Competency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompetencyRepositoryPort {
    Competency save(Competency competency);
    Optional<Competency> findById(UUID id);
    boolean existsByProgramIdAndSenaCode(UUID programId, String senaCode);
    boolean existsByProgramIdAndSenaCodeAndIdNot(UUID programId, String senaCode, UUID excludedId);
    List<Competency> findAll(UUID programId,
                             Optional<Boolean> isActive,
                             int page,
                             int size,
                             Optional<String> sort);
    long count(UUID programId, Optional<Boolean> isActive);
}
