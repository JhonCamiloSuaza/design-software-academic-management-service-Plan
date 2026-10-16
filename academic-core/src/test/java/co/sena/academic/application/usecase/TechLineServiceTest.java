package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.CreateTechLineUseCase;
import co.sena.academic.application.port.in.QueryTechLinesUseCase;
import co.sena.academic.application.port.in.UpdateTechLineUseCase;
import co.sena.academic.application.port.out.TechLineRepositoryPort;
import co.sena.academic.domain.exception.TechLineCodeAlreadyExistsException;
import co.sena.academic.domain.exception.TechLineNotFoundException;
import co.sena.academic.domain.model.TechLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TechLineServiceTest {

    private TechLineRepositoryPort repository;
    private TechLineService service;
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TechLineRepositoryPort.class);
        service = new TechLineService(repository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---- E1 ----
    @Test
    void e1_createsTheLineWhenCodeDoesNotExist() {
        when(repository.existsByCode("TIC")).thenReturn(false);

        TechLine result = service.create(new CreateTechLineUseCase.Command(
                "Information Technologies", "TIC", actorId));

        assertThat(result.getCode()).isEqualTo("TIC");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getCreatedBy()).isEqualTo(actorId);
    }

    @Test
    void e1_rejectsCreationWhenCodeAlreadyExists() {
        when(repository.existsByCode("TIC")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateTechLineUseCase.Command("Other", "TIC", actorId)))
                .isInstanceOf(TechLineCodeAlreadyExistsException.class);
    }

    // ---- E2 ----
    @Test
    void e2_throwsNotFoundWhenIdDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(TechLineNotFoundException.class);
    }

    @Test
    void e2_returnsTheRequestedPage() {
        when(repository.findAll(Optional.of(true), 0, 20)).thenReturn(java.util.List.of());
        when(repository.count(Optional.of(true))).thenReturn(0L);

        QueryTechLinesUseCase.Page page = service.list(
                new QueryTechLinesUseCase.Query(Optional.of(true), 0, 20));

        assertThat(page.totalElements()).isZero();
    }

    // ---- E3 ----
    @Test
    void e3_updatesWhenItExistsAndCodeDoesNotClashWithAnother() {
        UUID id = UUID.randomUUID();
        TechLine existing = TechLine.reconstitute(id, "Old", "OLD", true,
                java.time.Instant.now(), actorId, java.time.Instant.now(), actorId);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.existsByCodeAndIdNot("NEW", id)).thenReturn(false);

        TechLine result = service.update(new UpdateTechLineUseCase.Command(id, "New", "NEW", actorId));

        assertThat(result.getCode()).isEqualTo("NEW");
        assertThat(result.getUpdatedBy()).isEqualTo(actorId);
    }

    @Test
    void e3_throwsNotFoundWhenIdDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                new UpdateTechLineUseCase.Command(id, "N", "C", actorId)))
                .isInstanceOf(TechLineNotFoundException.class);
    }

    @Test
    void e3_rejectsWhenNewCodeClashesWithAnotherLine() {
        UUID id = UUID.randomUUID();
        TechLine existing = TechLine.reconstitute(id, "Old", "OLD", true,
                java.time.Instant.now(), actorId, java.time.Instant.now(), actorId);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.existsByCodeAndIdNot("DUP", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(
                new UpdateTechLineUseCase.Command(id, "New", "DUP", actorId)))
                .isInstanceOf(TechLineCodeAlreadyExistsException.class);
    }

    // ---- E4 ----
    @Test
    void e4_deactivatesAnExistingLine() {
        UUID id = UUID.randomUUID();
        TechLine existing = TechLine.reconstitute(id, "Old", "OLD", true,
                java.time.Instant.now(), actorId, java.time.Instant.now(), actorId);
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        service.deactivate(id, actorId);

        assertThat(existing.isActive()).isFalse();
        Mockito.verify(repository).save(existing);
    }

    @Test
    void e4_throwsNotFoundWhenIdDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(id, actorId))
                .isInstanceOf(TechLineNotFoundException.class);
    }
}