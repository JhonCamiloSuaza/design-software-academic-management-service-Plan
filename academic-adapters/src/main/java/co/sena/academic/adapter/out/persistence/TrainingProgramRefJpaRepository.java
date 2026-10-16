package co.sena.academic.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrainingProgramRefJpaRepository extends JpaRepository<TrainingProgramRefJpaEntity, UUID> {

    boolean existsByKnowledgeNetworkIdAndActive(UUID knowledgeNetworkId, boolean active);
}