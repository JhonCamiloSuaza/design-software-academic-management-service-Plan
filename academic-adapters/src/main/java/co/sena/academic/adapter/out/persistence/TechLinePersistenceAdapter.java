package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.out.TechLineRepositoryPort;
import co.sena.academic.domain.exception.TechLineCodeAlreadyExistsException;
import co.sena.academic.domain.model.TechLine;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TechLinePersistenceAdapter implements TechLineRepositoryPort {

    private final TechLineJpaRepository jpaRepository;

    public TechLinePersistenceAdapter(TechLineJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TechLine save(TechLine techLine) {
        try {
            TechLineJpaEntity saved = jpaRepository.save(TechLineJpaMapper.toEntity(techLine));
            return TechLineJpaMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            // Safety net against race conditions: two concurrent requests
            //can both pass the existsByCode() of the use case (E1)
            //before either commits. If that happens,
            //it's the actual constraint uq_tech_line_code that stops the
            //second insert/update — resulting in the same business 409
            //instead of letting a 500 escape.
            throw new TechLineCodeAlreadyExistsException(techLine.getCode());
        }
    }

    @Override
    public Optional<TechLine> findById(UUID id) {
        return jpaRepository.findById(id).map(TechLineJpaMapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, UUID excludedId) {
        return jpaRepository.existsByCodeAndIdNot(code, excludedId);
    }

    @Override
    public List<TechLine> findAll(Optional<Boolean> isActive, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        var result = isActive.isPresent()
                ? jpaRepository.findAllByActive(isActive.get(), pageRequest)
                : jpaRepository.findAll(pageRequest);
        return result.map(TechLineJpaMapper::toDomain).getContent();
    }

    @Override
    public long count(Optional<Boolean> isActive) {
        PageRequest pageRequest = PageRequest.of(0, 1);
        return isActive.isPresent()
                ? jpaRepository.findAllByActive(isActive.get(), pageRequest).getTotalElements()
                : jpaRepository.count();
    }
}