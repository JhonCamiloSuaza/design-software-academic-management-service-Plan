package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.out.TechLineLookupPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Read-only adapter for TechLineLookupPort, reusing the SAME
 * TechLineJpaRepository already owned by HU-ACAD-01 — no new entity,
 * no coupling beyond a plain lookup.
 */
@Component
public class TechLineLookupPersistenceAdapter implements TechLineLookupPort {

    private final TechLineJpaRepository techLineJpaRepository;

    public TechLineLookupPersistenceAdapter(TechLineJpaRepository techLineJpaRepository) {
        this.techLineJpaRepository = techLineJpaRepository;
    }

    @Override
    public boolean existsById(UUID id) {
        return techLineJpaRepository.existsById(id);
    }

    @Override
    public boolean isActiveById(UUID id) {
        return techLineJpaRepository.findById(id)
                .map(TechLineJpaEntity::isActive)
                .orElse(false);
    }
}