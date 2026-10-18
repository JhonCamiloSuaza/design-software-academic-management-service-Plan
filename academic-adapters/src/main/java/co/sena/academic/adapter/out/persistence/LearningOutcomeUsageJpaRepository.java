package co.sena.academic.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LearningOutcomeUsageJpaRepository extends JpaRepository<LearningOutcomeUsageJpaEntity, UUID> {
    boolean existsByCompetencyIdAndActiveTrue(UUID competencyId);
}
