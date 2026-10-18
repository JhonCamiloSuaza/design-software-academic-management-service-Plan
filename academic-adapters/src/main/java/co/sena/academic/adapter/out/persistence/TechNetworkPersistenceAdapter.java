package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.out.TechNetworkRepositoryPort;
import co.sena.academic.domain.exception.TechNetworkCodeAlreadyExistsException;
import co.sena.academic.domain.model.TechNetwork;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TechNetworkPersistenceAdapter implements TechNetworkRepositoryPort {

    private final TechNetworkJpaRepository jpaRepository;

    public TechNetworkPersistenceAdapter(TechNetworkJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TechNetwork save(TechNetwork techNetwork) {
        try {
            TechNetworkJpaEntity saved = jpaRepository.save(TechNetworkJpaMapper.toEntity(techNetwork));
            return TechNetworkJpaMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            // Same race-condition safety net as TechLinePersistenceAdapter.
            throw new TechNetworkCodeAlreadyExistsException(techNetwork.getTechLineId(), techNetwork.getCode());
        }
    }

    @Override
    public Optional<TechNetwork> findById(UUID id) {
        return jpaRepository.findById(id).map(TechNetworkJpaMapper::toDomain);
    }

    @Override
    public boolean existsByTechLineIdAndCode(UUID techLineId, String code) {
        return jpaRepository.existsByTechLineIdAndCode(techLineId, code);
    }

    @Override
    public boolean existsByTechLineIdAndCodeAndIdNot(UUID techLineId, String code, UUID excludedId) {
        return jpaRepository.existsByTechLineIdAndCodeAndIdNot(techLineId, code, excludedId);
    }

    @Override
    public List<TechNetwork> findAll(Optional<UUID> techLineId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        var result = techLineId.isPresent()
                ? jpaRepository.findAllByTechLineId(techLineId.get(), pageRequest)
                : jpaRepository.findAll(pageRequest);
        return result.map(TechNetworkJpaMapper::toDomain).getContent();
    }

    @Override
    public long count(Optional<UUID> techLineId) {
        return techLineId.isPresent()
                ? jpaRepository.countByTechLineId(techLineId.get())
                : jpaRepository.count();
    }
}