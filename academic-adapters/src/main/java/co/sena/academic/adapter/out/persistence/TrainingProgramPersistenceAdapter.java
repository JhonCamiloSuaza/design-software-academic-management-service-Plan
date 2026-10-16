package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.out.TrainingProgramRepositoryPort;
import co.sena.academic.domain.exception.RowVersionMismatchException;
import co.sena.academic.domain.exception.TrainingProgramCodeAlreadyExistsException;
import co.sena.academic.domain.model.TrainingProgram;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TrainingProgramPersistenceAdapter implements TrainingProgramRepositoryPort {

    private final TrainingProgramJpaRepository jpaRepository;

    public TrainingProgramPersistenceAdapter(TrainingProgramJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TrainingProgram save(TrainingProgram trainingProgram) {
        try {
            TrainingProgramJpaEntity saved = jpaRepository.saveAndFlush(
                    TrainingProgramJpaMapper.toEntity(trainingProgram));
            return TrainingProgramJpaMapper.toDomain(saved);
        } catch (OptimisticLockingFailureException ex) {
            throw new RowVersionMismatchException(trainingProgram.getId());
        } catch (DataIntegrityViolationException ex) {
            // All FK/CHECK rules used by HU-ACAD-04 are validated before save.
            // The remaining expected integrity race is the global program_code unique constraint.
            throw new TrainingProgramCodeAlreadyExistsException(trainingProgram.getProgramCode());
        }
    }

    @Override
    public Optional<TrainingProgram> findById(UUID id) {
        return jpaRepository.findById(id).map(TrainingProgramJpaMapper::toDomain);
    }

    @Override
    public boolean existsByProgramCode(String programCode) {
        return jpaRepository.existsByProgramCode(programCode);
    }

    @Override
    public boolean existsByProgramCodeAndIdNot(String programCode, UUID excludedId) {
        return jpaRepository.existsByProgramCodeAndIdNot(programCode, excludedId);
    }

    @Override
    public List<TrainingProgram> findAll(Optional<UUID> knowledgeNetworkId,
                                         Optional<String> programCode,
                                         Optional<String> trainingLevel,
                                         Optional<Boolean> isActive,
                                         int page,
                                         int size,
                                         Optional<String> sort) {
        PageRequest pageRequest = PageRequest.of(page, size, resolveSort(sort));
        Page<TrainingProgramJpaEntity> result = jpaRepository.findAll(
                buildSpecification(knowledgeNetworkId, programCode, trainingLevel, isActive),
                pageRequest);
        return result.map(TrainingProgramJpaMapper::toDomain).getContent();
    }

    @Override
    public long count(Optional<UUID> knowledgeNetworkId,
                      Optional<String> programCode,
                      Optional<String> trainingLevel,
                      Optional<Boolean> isActive) {
        return jpaRepository.count(buildSpecification(
                knowledgeNetworkId, programCode, trainingLevel, isActive));
    }

    private static Specification<TrainingProgramJpaEntity> buildSpecification(
            Optional<UUID> knowledgeNetworkId,
            Optional<String> programCode,
            Optional<String> trainingLevel,
            Optional<Boolean> isActive) {
        Specification<TrainingProgramJpaEntity> specification = Specification.where(null);
        if (knowledgeNetworkId.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("knowledgeNetworkId"), knowledgeNetworkId.get()));
        }
        if (programCode.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("programCode"), programCode.get()));
        }
        if (trainingLevel.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("trainingLevel"), trainingLevel.get()));
        }
        if (isActive.isPresent()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("active"), isActive.get()));
        }
        return specification;
    }

    private static org.springframework.data.domain.Sort resolveSort(Optional<String> sort) {
        org.springframework.data.domain.Sort defaultSort =
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        if (sort.isEmpty() || sort.get().isBlank()) {
            return defaultSort;
        }

        String[] parts = sort.get().split(":", 2);
        String property = switch (parts[0]) {
            case "name" -> "name";
            case "program_code" -> "programCode";
            case "training_level" -> "trainingLevel";
            case "total_hours" -> "totalHours";
            case "version" -> "version";
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
