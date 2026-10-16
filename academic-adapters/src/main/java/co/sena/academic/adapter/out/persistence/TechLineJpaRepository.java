package co.sena.academic.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TechLineJpaRepository extends JpaRepository<TechLineJpaEntity, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    Page<TechLineJpaEntity> findAllByActive(boolean active, Pageable pageable);

    // findAll(Pageable) is already inherited from JpaRepository for the no-filter case.
}