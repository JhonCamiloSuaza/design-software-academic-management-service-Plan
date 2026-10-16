package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.out.CompetencyRepositoryPort;
import co.sena.academic.domain.exception.CompetencyCodeAlreadyExistsException;
import co.sena.academic.domain.exception.RowVersionMismatchException;
import co.sena.academic.domain.model.Competency;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CompetencyPersistenceAdapter implements CompetencyRepositoryPort {

    private final CompetencyJpaRepository jpaRepository;

    public CompetencyPersistenceAdapter(CompetencyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Competency save(Competency competency) {
        try {
            var saved = jpaRepository.saveAndFlush(CompetencyJpaMapper.toEntity(competency));
            return CompetencyJpaMapper.toDomain(saved);
        } catch (OptimisticLockingFailureException ex) {
            throw new RowVersionMismatchException(competency.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new CompetencyCodeAlreadyExistsException(
                    competency.getProgramId(), competency.getSenaCode());
        }
    }

    @Override
    public Optional<Competency> findById(UUID id) {
        return jpaRepository.findById(id).map(CompetencyJpaMapper::toDomain);
    }

    @Override
    public boolean existsByProgramIdAndSenaCode(UUID programId, String senaCode) {
        return jpaRepository.existsByProgramIdAndSenaCode(programId, senaCode);
    }

    @Override
    public boolean existsByProgramIdAndSenaCodeAndIdNot(
            UUID programId, String senaCode, UUID excludedId) {
        return jpaRepository.existsByProgramIdAndSenaCodeAndIdNot(programId, senaCode, excludedId);
    }

    @Override
    public List<Competency> findAll(UUID programId,
                                    Optional<Boolean> isActive,
                                    int page,
                                    int size,
                                    Optional<String> sort) {
        var result = jpaRepository.findAll(
                buildSpecification(programId, isActive),
                PageRequest.of(page, size, resolveSort(sort)));
        return result.map(CompetencyJpaMapper::toDomain).getContent();
    }

    @Override
    public long count(UUID programId, Optional<Boolean> isActive) {
        return jpaRepository.count(buildSpecification(programId, isActive));
    }

    private static Specification<CompetencyJpaEntity> buildSpecification(
            UUID programId, Optional<Boolean> isActive) {
        Specification<CompetencyJpaEntity> specification =
                (root, query, cb) -> cb.equal(root.get("programId"), programId);
        if (isActive.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("active"), isActive.get()));
        }
        return specification;
    }

    private static org.springframework.data.domain.Sort resolveSort(Optional<String> sort) {
        var defaultSort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        if (sort.isEmpty() || sort.get().isBlank()) {
            return defaultSort;
        }
        String[] parts = sort.get().split(":", 2);
        String property = switch (parts[0]) {
            case "name" -> "name";
            case "sena_code" -> "senaCode";
            case "hours" -> "hours";
            case "is_active" -> "active";
            case "created_at" -> "createdAt";
            case "updated_at" -> "updatedAt";
            default -> null;
        };
        if (property == null) {
            return defaultSort;
        }
        var direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? org.springframework.data.domain.Sort.Direction.DESC
                : org.springframework.data.domain.Sort.Direction.ASC;
        return org.springframework.data.domain.Sort.by(direction, property);
    }
}
