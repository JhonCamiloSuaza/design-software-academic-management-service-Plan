package co.sena.academic.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TrainingProgramJpaRepository extends
        JpaRepository<TrainingProgramJpaEntity, UUID>,
        JpaSpecificationExecutor<TrainingProgramJpaEntity> {

    boolean existsByProgramCode(String programCode);

    boolean existsByProgramCodeAndIdNot(String programCode, UUID id);
}
