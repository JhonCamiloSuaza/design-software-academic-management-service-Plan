package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.out.KnowledgeNetworkRepositoryPort;
import co.sena.academic.domain.exception.KnowledgeNetworkCodeAlreadyExistsException;
import co.sena.academic.domain.model.KnowledgeNetwork;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class KnowledgeNetworkPersistenceAdapter implements KnowledgeNetworkRepositoryPort {

    private final KnowledgeNetworkJpaRepository jpaRepository;

    public KnowledgeNetworkPersistenceAdapter(KnowledgeNetworkJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public KnowledgeNetwork save(KnowledgeNetwork knowledgeNetwork) {
        try {
            KnowledgeNetworkJpaEntity saved = jpaRepository.save(KnowledgeNetworkJpaMapper.toEntity(knowledgeNetwork));
            return KnowledgeNetworkJpaMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            // Safety net against race conditions: two concurrent requests can
            // both pass the existsByTechNetworkIdAndCode() of the use case (E1)
            // before either commits. If that happens, it's the actual
            // uq_knowledge_network_tech_network_id_code constraint that stops
            // the second insert/update — resulting in the same business 409
            // instead of letting a 500 escape.
            throw new KnowledgeNetworkCodeAlreadyExistsException(knowledgeNetwork.getTechNetworkId(), knowledgeNetwork.getCode());
        }
    }

    @Override
    public Optional<KnowledgeNetwork> findById(UUID id) {
        return jpaRepository.findById(id).map(KnowledgeNetworkJpaMapper::toDomain);
    }

    @Override
    public boolean existsByTechNetworkIdAndCode(UUID techNetworkId, String code) {
        return jpaRepository.existsByTechNetworkIdAndCode(techNetworkId, code);
    }

    @Override
    public boolean existsByTechNetworkIdAndCodeAndIdNot(UUID techNetworkId, String code, UUID excludedId) {
        return jpaRepository.existsByTechNetworkIdAndCodeAndIdNot(techNetworkId, code, excludedId);
    }

@Override
public List<KnowledgeNetwork> findAll(Optional<UUID> techNetworkId, Optional<Boolean> isActive,
                                       int page, int size, Optional<String> sort) {
    PageRequest pageRequest = PageRequest.of(page, size, resolveSort(sort));

    Page<KnowledgeNetworkJpaEntity> result;
    if (techNetworkId.isPresent() && isActive.isPresent()) {
        result = jpaRepository.findAllByTechNetworkIdAndActive(techNetworkId.get(), isActive.get(), pageRequest);
    } else if (techNetworkId.isPresent()) {
        result = jpaRepository.findAllByTechNetworkId(techNetworkId.get(), pageRequest);
    } else if (isActive.isPresent()) {
        result = jpaRepository.findAllByActive(isActive.get(), pageRequest);
    } else {
        result = jpaRepository.findAll(pageRequest);
    }
    return result.map(KnowledgeNetworkJpaMapper::toDomain).getContent();
}

@Override
public long count(Optional<UUID> techNetworkId, Optional<Boolean> isActive) {
    if (techNetworkId.isPresent() && isActive.isPresent()) {
        return jpaRepository.countByTechNetworkIdAndActive(techNetworkId.get(), isActive.get());
    } else if (techNetworkId.isPresent()) {
        return jpaRepository.countByTechNetworkId(techNetworkId.get());
    } else if (isActive.isPresent()) {
        return jpaRepository.countByActive(isActive.get());
    } else {
        return jpaRepository.count();
    }
}

/**
 * Traduce "campo:asc|desc" (contrato: _shared.yaml#/components/parameters/sort)
 * a un Sort real, con allow-list de campos (el guidelines.md deja esa
 * validación a criterio del microservicio). Campo desconocido o sort
 * ausente -> orden por defecto (created_at desc), en vez de fallar con 500.
 */
private static org.springframework.data.domain.Sort resolveSort(Optional<String> sort) {
    org.springframework.data.domain.Sort defaultSort =
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");

    if (sort.isEmpty() || sort.get().isBlank()) {
        return defaultSort;
    }
    String[] parts = sort.get().split(":", 2);
    String property = switch (parts[0]) {
        case "name" -> "name";
        case "code" -> "code";
        case "created_at" -> "createdAt";
        case "is_active" -> "active";
        default -> null;
    };
    if (property == null) {
        return defaultSort;
    }
    var direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1]))
            ? org.springframework.data.domain.Sort.Direction.DESC
            : org.springframework.data.domain.Sort.Direction.ASC;
    return org.springframework.data.domain.Sort.by(direction, property);
}
}