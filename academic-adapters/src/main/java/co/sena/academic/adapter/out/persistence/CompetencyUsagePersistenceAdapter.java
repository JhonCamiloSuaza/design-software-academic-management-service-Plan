package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.out.CompetencyUsagePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * HU-ACAD-06 E3 implementation using the current academic schema.
 *
 * learning_outcome belongs to competency, competency belongs to training_program,
 * and enrollment_ficha freezes that training_program. Therefore a competency is
 * considered in use when it has an active learning outcome and the same program
 * has a non-terminal ficha.
 */
@Component
public class CompetencyUsagePersistenceAdapter implements CompetencyUsagePort {

    private static final List<String> OPEN_FICHA_STATUSES = List.of(
            "INDUCTION", "EXECUTION", "PRODUCTIVE_STAGE");

    private final LearningOutcomeUsageJpaRepository learningOutcomeRepository;
    private final FichaJpaRepository fichaRepository;

    public CompetencyUsagePersistenceAdapter(LearningOutcomeUsageJpaRepository learningOutcomeRepository,
                                             FichaJpaRepository fichaRepository) {
        this.learningOutcomeRepository = learningOutcomeRepository;
        this.fichaRepository = fichaRepository;
    }

    @Override
    public boolean isUsedByOpenFichas(UUID competencyId, UUID programId) {
        return learningOutcomeRepository.existsByCompetencyIdAndActiveTrue(competencyId)
                && fichaRepository.existsByProgramIdAndStatusIn(programId, OPEN_FICHA_STATUSES);
    }
}
