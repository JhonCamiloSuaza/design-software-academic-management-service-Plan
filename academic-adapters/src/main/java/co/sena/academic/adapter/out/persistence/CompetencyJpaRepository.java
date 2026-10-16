package co.sena.academic.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CompetencyJpaRepository extends
        JpaRepository<CompetencyJpaEntity, UUID>,
        JpaSpecificationExecutor<CompetencyJpaEntity> {

    boolean existsByProgramIdAndSenaCode(UUID programId, String senaCode);

    boolean existsByProgramIdAndSenaCodeAndIdNot(UUID programId, String senaCode, UUID id);
}
