package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.out.FichaRepositoryPort;
import co.sena.academic.domain.exception.FichaNumberAlreadyExistsException;
import co.sena.academic.domain.model.Ficha;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class FichaPersistenceAdapter implements FichaRepositoryPort {

    private final FichaJpaRepository repository;

    public FichaPersistenceAdapter(FichaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Ficha save(Ficha ficha) {
        try {
            return FichaJpaMapper.toDomain(repository.saveAndFlush(FichaJpaMapper.toEntity(ficha)));
        } catch (OptimisticLockingFailureException ex) {
            throw new co.sena.academic.domain.exception.RowVersionMismatchException(ficha.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new FichaNumberAlreadyExistsException(ficha.getFichaNumber());
        }
    }

    @Override
    public boolean existsByFichaNumber(String fichaNumber) {
        return repository.existsByFichaNumber(fichaNumber);
    }

    @Override
    public Optional<Ficha> findById(UUID id) {
        return repository.findById(id).map(FichaJpaMapper::toDomain);
    }

    @Override
    public List<Ficha> findAll(Optional<UUID> programId,
                               Optional<String> status,
                               Optional<String> fichaNumber,
                               Optional<UUID> trainingCenterId,
                               Optional<LocalDate> from,
                               Optional<LocalDate> to,
                               Optional<Set<UUID>> allowedFichaIds,
                               int page,
                               int size,
                               Optional<String> sort) {
        PageRequest pageRequest = PageRequest.of(page, size, resolveSort(sort));
        Page<FichaJpaEntity> result = repository.findAll(
                buildSpecification(programId, status, fichaNumber, trainingCenterId,
                        from, to, allowedFichaIds), pageRequest);
        return result.map(FichaJpaMapper::toDomain).getContent();
    }

    @Override
    public long count(Optional<UUID> programId,
                      Optional<String> status,
                      Optional<String> fichaNumber,
                      Optional<UUID> trainingCenterId,
                      Optional<LocalDate> from,
                      Optional<LocalDate> to,
                      Optional<Set<UUID>> allowedFichaIds) {
        return repository.count(buildSpecification(
                programId, status, fichaNumber, trainingCenterId, from, to, allowedFichaIds));
    }

    private static Specification<FichaJpaEntity> buildSpecification(
            Optional<UUID> programId,
            Optional<String> status,
            Optional<String> fichaNumber,
            Optional<UUID> trainingCenterId,
            Optional<LocalDate> from,
            Optional<LocalDate> to,
            Optional<Set<UUID>> allowedFichaIds) {
        Specification<FichaJpaEntity> specification = Specification.where(null);
        if (programId.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("programId"), programId.get()));
        }
        if (status.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("status"), status.get()));
        }
        if (fichaNumber.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("fichaNumber"), fichaNumber.get()));
        }
        if (trainingCenterId.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("trainingCenterId"), trainingCenterId.get()));
        }
        if (from.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("startDate"), from.get()));
        }
        if (to.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThan(root.get("startDate"), to.get()));
        }
        if (allowedFichaIds.isPresent()) {
            if (allowedFichaIds.get().isEmpty()) {
                specification = specification.and((root, query, cb) -> cb.disjunction());
            } else {
                specification = specification.and((root, query, cb) ->
                        root.get("id").in(allowedFichaIds.get()));
            }
        }
        return specification;
    }

    private static Sort resolveSort(Optional<String> sort) {
        Sort defaultSort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort.isEmpty() || sort.get().isBlank()) {
            return defaultSort;
        }

        String[] parts = sort.get().split(":", 2);
        String property = switch (parts[0]) {
            case "ficha_number" -> "fichaNumber";
            case "program_id" -> "programId";
            case "program_version" -> "programVersion";
            case "training_center_id" -> "trainingCenterId";
            case "status" -> "status";
            case "start_date" -> "startDate";
            case "expected_end_date" -> "expectedEndDate";
            case "created_at" -> "createdAt";
            case "updated_at" -> "updatedAt";
            default -> null;
        };
        if (property == null) {
            return defaultSort;
        }

        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}
