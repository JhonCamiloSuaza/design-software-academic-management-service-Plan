package co.sena.academic.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.UUID;

public interface FichaJpaRepository extends
        JpaRepository<FichaJpaEntity, UUID>,
        JpaSpecificationExecutor<FichaJpaEntity> {
    boolean existsByFichaNumber(String fichaNumber);

    boolean existsByProgramIdAndStatusIn(UUID programId, Collection<String> statuses);
}
