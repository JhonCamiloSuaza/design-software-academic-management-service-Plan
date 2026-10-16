package co.sena.academic.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KnowledgeNetworkJpaRepository extends JpaRepository<KnowledgeNetworkJpaEntity, UUID> {

    boolean existsByTechNetworkIdAndCode(UUID techNetworkId, String code);

    boolean existsByTechNetworkIdAndCodeAndIdNot(UUID techNetworkId, String code, UUID id);

    Page<KnowledgeNetworkJpaEntity> findAllByTechNetworkId(UUID techNetworkId, Pageable pageable);

    Page<KnowledgeNetworkJpaEntity> findAllByTechNetworkIdAndActive(UUID techNetworkId, boolean active, Pageable pageable);
    Page<KnowledgeNetworkJpaEntity> findAllByActive(boolean active, Pageable pageable);

    long countByTechNetworkIdAndActive(UUID techNetworkId, boolean active);
    long countByTechNetworkId(UUID techNetworkId);
    long countByActive(boolean active);
}