package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.CreateLearningOutcomeUseCase;
import co.sena.academic.application.port.in.DeactivateLearningOutcomeUseCase;
import co.sena.academic.application.port.in.QueryLearningOutcomesUseCase;
import co.sena.academic.application.port.in.UpdateLearningOutcomeUseCase;
import co.sena.academic.application.port.out.CompetencyRepositoryPort;
import co.sena.academic.application.port.out.LearningOutcomeRepositoryPort;
import co.sena.academic.domain.exception.CompetencyDeactivatedException;
import co.sena.academic.domain.exception.LearningOutcomeCodeAlreadyExistsException;
import co.sena.academic.domain.exception.RowVersionMismatchException;
import co.sena.academic.domain.model.Competency;
import co.sena.academic.domain.model.LearningOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LearningOutcomeServiceTest {

    private LearningOutcomeRepositoryPort repository;
    private CompetencyRepositoryPort competencyRepository;
    private LearningOutcomeService service;
    private final UUID actorId = UUID.randomUUID();
    private final UUID competencyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LearningOutcomeRepositoryPort.class);
        competencyRepository = Mockito.mock(CompetencyRepositoryPort.class);
        service = new LearningOutcomeService(repository, competencyRepository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsAnOutcomeForAnActiveCompetencyWhenCodeIsFree() {
        when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(activeCompetency()));
        when(repository.existsByCompetencyIdAndCode(competencyId, "LO-01")).thenReturn(false);

        LearningOutcome result = service.create(new CreateLearningOutcomeUseCase.Command(
                competencyId, "LO-01", "Describe a solution", true, actorId));

        assertThat(result.getCompetencyId()).isEqualTo(competencyId);
        assertThat(result.getCode()).isEqualTo("LO-01");
        assertThat(result.getDescription()).isEqualTo("Describe a solution");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getCreatedBy()).isEqualTo(actorId);
    }

    @Test
    void rejectsCreationWhenCodeAlreadyExistsForTheCompetency() {
        when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(activeCompetency()));
        when(repository.existsByCompetencyIdAndCode(competencyId, "LO-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateLearningOutcomeUseCase.Command(
                competencyId, "LO-01", "Duplicate", true, actorId)))
                .isInstanceOf(LearningOutcomeCodeAlreadyExistsException.class);
    }

    @Test
    void rejectsCreationForADeactivatedCompetency() {
        when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(deactivatedCompetency()));

        assertThatThrownBy(() -> service.create(new CreateLearningOutcomeUseCase.Command(
                competencyId, "LO-01", "Not allowed", true, actorId)))
                .isInstanceOf(CompetencyDeactivatedException.class);

        Mockito.verifyNoInteractions(repository);
    }

    @Test
    void updatesAnOutcomeWhenRowVersionMatches() {
        UUID outcomeId = UUID.randomUUID();
        LearningOutcome existing = outcome(outcomeId, true, 2);
        when(repository.findById(outcomeId)).thenReturn(Optional.of(existing));
        when(repository.existsByCompetencyIdAndCodeAndIdNot(competencyId, "LO-02", outcomeId))
                .thenReturn(false);

        LearningOutcome result = service.update(new UpdateLearningOutcomeUseCase.Command(
                outcomeId, "LO-02", "Updated description", true, 2, actorId));

        assertThat(result.getCode()).isEqualTo("LO-02");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getUpdatedBy()).isEqualTo(actorId);
        Mockito.verify(repository).save(existing);
    }

    @Test
    void rejectsUpdateWhenRowVersionDoesNotMatch() {
        UUID outcomeId = UUID.randomUUID();
        when(repository.findById(outcomeId)).thenReturn(Optional.of(outcome(outcomeId, true, 2)));

        assertThatThrownBy(() -> service.update(new UpdateLearningOutcomeUseCase.Command(
                outcomeId, "LO-02", "Updated description", true, 1, actorId)))
                .isInstanceOf(RowVersionMismatchException.class);

        Mockito.verify(repository, Mockito.never()).save(any());
    }

    @Test
    void deactivatesAnOutcomeWithoutDeletingItsAuditData() {
        UUID outcomeId = UUID.randomUUID();
        LearningOutcome existing = outcome(outcomeId, true, 0);
        when(repository.findById(outcomeId)).thenReturn(Optional.of(existing));

        service.deactivate(new DeactivateLearningOutcomeUseCase.Command(outcomeId, actorId));

        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getDeletedBy()).isEqualTo(actorId);
        Mockito.verify(repository).save(existing);
    }

    @Test
    void returnsTheRequestedPageScopedToTheCompetency() {
        List<LearningOutcome> outcomes = List.of(outcome(UUID.randomUUID(), true, 0));
        Optional<Boolean> active = Optional.of(true);
        Optional<String> sort = Optional.of("code:asc");
        when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(activeCompetency()));
        when(repository.findAll(competencyId, active, 1, 10, sort)).thenReturn(outcomes);
        when(repository.count(competencyId, active)).thenReturn(11L);

        QueryLearningOutcomesUseCase.Page page = service.list(
                new QueryLearningOutcomesUseCase.Query(competencyId, active, 1, 10, sort));

        assertThat(page.content()).containsExactlyElementsOf(outcomes);
        assertThat(page.totalElements()).isEqualTo(11L);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(10);
    }

    private Competency activeCompetency() {
        return Competency.create(UUID.randomUUID(), "Competency", "COMP-01", 40, true, actorId);
    }

    private Competency deactivatedCompetency() {
        Competency competency = activeCompetency();
        competency.deactivate(actorId);
        return competency;
    }

    private LearningOutcome outcome(UUID id, boolean active, int rowVersion) {
        Instant now = Instant.now();
        return LearningOutcome.reconstitute(id, competencyId, "LO-01", "Description", active, rowVersion,
                now, actorId, now, actorId, null, null);
    }
}
