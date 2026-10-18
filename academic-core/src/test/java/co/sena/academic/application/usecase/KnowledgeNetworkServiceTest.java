package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.CreateKnowledgeNetworkUseCase;
import co.sena.academic.application.port.in.QueryKnowledgeNetworksUseCase;
import co.sena.academic.application.port.in.UpdateKnowledgeNetworkUseCase;
import co.sena.academic.application.port.out.KnowledgeNetworkRepositoryPort;
import co.sena.academic.application.port.out.TechNetworkLookupPort;
import co.sena.academic.application.port.out.TrainingProgramLookupPort;
import co.sena.academic.domain.exception.KnowledgeNetworkCodeAlreadyExistsException;
import co.sena.academic.domain.exception.KnowledgeNetworkHasActiveTrainingProgramsException;
import co.sena.academic.domain.exception.KnowledgeNetworkNotFoundException;
import co.sena.academic.domain.exception.TechNetworkNotFoundException;
import co.sena.academic.domain.model.KnowledgeNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class KnowledgeNetworkServiceTest {

    private KnowledgeNetworkRepositoryPort repository;
    private TechNetworkLookupPort techNetworkLookupPort;
    private TrainingProgramLookupPort trainingProgramLookupPort;
    private KnowledgeNetworkService service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID techNetworkId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(KnowledgeNetworkRepositoryPort.class);
        techNetworkLookupPort = Mockito.mock(TechNetworkLookupPort.class);
        trainingProgramLookupPort = Mockito.mock(TrainingProgramLookupPort.class);
        service = new KnowledgeNetworkService(repository, techNetworkLookupPort, trainingProgramLookupPort);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---- E1: create ----
    @Test
    void e1_createsTheNetworkWhenTechNetworkExistsAndCodeIsFree() {
        when(techNetworkLookupPort.existsById(techNetworkId)).thenReturn(true);
        when(repository.existsByTechNetworkIdAndCode(techNetworkId, "RED-SW")).thenReturn(false);

        KnowledgeNetwork result = service.create(new CreateKnowledgeNetworkUseCase.Command(
                techNetworkId, "Software Development", "RED-SW", actorId));

        assertThat(result.getCode()).isEqualTo("RED-SW");
        assertThat(result.getTechNetworkId()).isEqualTo(techNetworkId);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getCreatedBy()).isEqualTo(actorId);
    }

    @Test
    void e1_rejectsCreationWhenTechNetworkDoesNotExist() {
        when(techNetworkLookupPort.existsById(techNetworkId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new CreateKnowledgeNetworkUseCase.Command(techNetworkId, "N", "C", actorId)))
                .isInstanceOf(TechNetworkNotFoundException.class);

        Mockito.verifyNoInteractions(repository);
    }

    @Test
    void e1_rejectsCreationWhenCodeAlreadyExistsForTheSameTechNetwork() {
        when(techNetworkLookupPort.existsById(techNetworkId)).thenReturn(true);
        when(repository.existsByTechNetworkIdAndCode(techNetworkId, "RED-SW")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateKnowledgeNetworkUseCase.Command(techNetworkId, "Other", "RED-SW", actorId)))
                .isInstanceOf(KnowledgeNetworkCodeAlreadyExistsException.class);
    }

    // ---- E2: query ----
    @Test
    void e2_throwsNotFoundWhenIdDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(KnowledgeNetworkNotFoundException.class);
    }

    @Test
    void e2_returnsTheRequestedPageScopedToTechNetworkId() {
        when(repository.findAll(Optional.of(techNetworkId), Optional.of(true), 0, 20, Optional.empty()))
                .thenReturn(java.util.List.of());
        when(repository.count(Optional.of(techNetworkId), Optional.of(true))).thenReturn(0L);

        QueryKnowledgeNetworksUseCase.Page page = service.list(
                new QueryKnowledgeNetworksUseCase.Query(techNetworkId, Optional.of(true), 0, 20, null));

        assertThat(page.totalElements()).isZero();
    }

    // ---- update ----
    @Test
    void update_replacesNameAndCodeWhenNoClashWithinTheSameTechNetwork() {
        UUID id = UUID.randomUUID();
        KnowledgeNetwork existing = KnowledgeNetwork.reconstitute(id, techNetworkId, "Old", "OLD", true,
                Instant.now(), actorId, Instant.now(), actorId);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.existsByTechNetworkIdAndCodeAndIdNot(techNetworkId, "NEW", id)).thenReturn(false);

        KnowledgeNetwork result = service.update(
                new UpdateKnowledgeNetworkUseCase.Command(id, techNetworkId, "New", "NEW", true, actorId));

        assertThat(result.getCode()).isEqualTo("NEW");
        assertThat(result.getTechNetworkId()).isEqualTo(techNetworkId);
        assertThat(result.getUpdatedBy()).isEqualTo(actorId);
    }

    @Test
    void update_throwsNotFoundWhenIdDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                new UpdateKnowledgeNetworkUseCase.Command(id, techNetworkId, "N", "C", true, actorId)))
                .isInstanceOf(KnowledgeNetworkNotFoundException.class);
    }

    @Test
    void update_rejectsWhenNewCodeClashesWithAnotherNetworkInTheSameTechNetwork() {
        UUID id = UUID.randomUUID();
        KnowledgeNetwork existing = KnowledgeNetwork.reconstitute(id, techNetworkId, "Old", "OLD", true,
                Instant.now(), actorId, Instant.now(), actorId);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.existsByTechNetworkIdAndCodeAndIdNot(techNetworkId, "DUP", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(
                new UpdateKnowledgeNetworkUseCase.Command(id, techNetworkId, "New", "DUP", true, actorId)))
                .isInstanceOf(KnowledgeNetworkCodeAlreadyExistsException.class);
    }

    // ---- E3: deactivate ("baja") ----
    @Test
    void e3_deactivatesWhenThereAreNoActiveTrainingProgramDependents() {
        UUID id = UUID.randomUUID();
        KnowledgeNetwork existing = KnowledgeNetwork.reconstitute(id, techNetworkId, "Old", "OLD", true,
                Instant.now(), actorId, Instant.now(), actorId);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(trainingProgramLookupPort.existsActiveByKnowledgeNetworkId(id)).thenReturn(false);

        service.deactivate(id, actorId);

        assertThat(existing.isActive()).isFalse();
        Mockito.verify(repository).save(existing);
    }

    @Test
    void e3_rejectsDeactivationWhenThereAreActiveTrainingProgramDependents() {
        UUID id = UUID.randomUUID();
        KnowledgeNetwork existing = KnowledgeNetwork.reconstitute(id, techNetworkId, "Old", "OLD", true,
                Instant.now(), actorId, Instant.now(), actorId);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(trainingProgramLookupPort.existsActiveByKnowledgeNetworkId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deactivate(id, actorId))
                .isInstanceOf(KnowledgeNetworkHasActiveTrainingProgramsException.class);

        assertThat(existing.isActive()).isTrue();
        Mockito.verify(repository, Mockito.never()).save(any());
    }

    @Test
    void e3_throwsNotFoundWhenIdDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(id, actorId))
                .isInstanceOf(KnowledgeNetworkNotFoundException.class);
    }
}