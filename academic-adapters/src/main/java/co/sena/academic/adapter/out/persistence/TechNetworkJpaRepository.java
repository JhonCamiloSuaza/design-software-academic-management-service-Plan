package co.sena.academic.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TechNetworkJpaRepository extends JpaRepository<TechNetworkJpaEntity, UUID> {

    boolean existsByTechLineIdAndCode(UUID techLineId, String code);

    boolean existsByTechLineIdAndCodeAndIdNot(UUID techLineId, String code, UUID id);

    Page<TechNetworkJpaEntity> findAllByTechLineId(UUID techLineId, Pageable pageable);

    long countByTechLineId(UUID techLineId);

    // findAll(Pageable)/count() inherited from JpaRepository for the no-filter case.
}