package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.CreateFichaUseCase;
import co.sena.academic.application.port.in.ChangeFichaStatusUseCase;
import co.sena.academic.application.port.in.QueryFichasUseCase;
import co.sena.academic.application.port.in.UpdateFichaUseCase;
import co.sena.academic.application.port.out.FichaEventPublisherPort;
import co.sena.academic.application.port.out.FichaRepositoryPort;
import co.sena.academic.application.port.out.FichaOwnershipPort;
import co.sena.academic.application.port.out.TrainingCenterLookupPort;
import co.sena.academic.application.port.out.TrainingProgramRepositoryPort;
import co.sena.academic.domain.exception.FichaNumberAlreadyExistsException;
import co.sena.academic.domain.exception.FichaCapacityIncreaseNotAllowedException;
import co.sena.academic.domain.exception.FichaStartDateMustBeFutureException;
import co.sena.academic.domain.exception.FichaTerminalStatusException;
import co.sena.academic.domain.exception.FichaStatusRoleDeniedException;
import co.sena.academic.domain.exception.InactiveTrainingCenterException;
import co.sena.academic.domain.exception.RowVersionMismatchException;
import co.sena.academic.domain.exception.TrainingCenterNotFoundException;
import co.sena.academic.domain.model.Ficha;
import co.sena.academic.domain.model.TrainingProgram;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FichaServiceTest {

    private FichaRepositoryPort fichaRepository;
    private TrainingProgramRepositoryPort trainingProgramRepository;
    private TrainingCenterLookupPort trainingCenterLookup;
    private FichaEventPublisherPort eventPublisher;
    private FichaOwnershipPort fichaOwnershipPort;
    private FichaService service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID programId = UUID.randomUUID();
    private final UUID centerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        fichaRepository = mock(FichaRepositoryPort.class);
        trainingProgramRepository = mock(TrainingProgramRepositoryPort.class);
        trainingCenterLookup = mock(TrainingCenterLookupPort.class);
        eventPublisher = mock(FichaEventPublisherPort.class);
        fichaOwnershipPort = mock(FichaOwnershipPort.class);
        service = new FichaService(
                fichaRepository,
                trainingProgramRepository,
                trainingCenterLookup,
                eventPublisher,
                fichaOwnershipPort);
        when(fichaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainingProgramRepository.findById(programId)).thenReturn(Optional.of(program(3, true)));
        when(trainingCenterLookup.findById(centerId))
                .thenReturn(Optional.of(new TrainingCenterLookupPort.TrainingCenterReference(true)));
    }

    @Test
    void createsFichaWithCurrentProgramVersionAndPublishesOpenedEvent() {
        Ficha ficha = service.create(command("F-100"));

        assertThat(ficha.getProgramVersion()).isEqualTo(3);
        assertThat(ficha.getStatus()).isEqualTo("INDUCTION");
        assertThat(ficha.getRowVersion()).isZero();

        verify(fichaRepository).save(any(Ficha.class));
        verify(eventPublisher).publish(argThat(event ->
                event.event_type().equals("academic.ficha.opened")
                        && event.payload().program_version() == 3
                        && event.payload().ficha_id().equals(ficha.getId())));
    }

    @Test
    void rejectsStartDateThatIsNotFuture() {
        assertThatThrownBy(() -> service.create(new CreateFichaUseCase.Command(
                "F-101", programId, centerId, LocalDate.now(), null,
                "DAY", "IN_PERSON", 20, actorId)))
                .isInstanceOf(FichaStartDateMustBeFutureException.class);

        verifyNoInteractions(trainingProgramRepository, trainingCenterLookup, fichaRepository, eventPublisher);
    }

    @Test
    void rejectsInactiveCenter() {
        when(trainingCenterLookup.findById(centerId))
                .thenReturn(Optional.of(new TrainingCenterLookupPort.TrainingCenterReference(false)));

        assertThatThrownBy(() -> service.create(command("F-102")))
                .isInstanceOf(InactiveTrainingCenterException.class);

        verifyNoInteractions(fichaRepository, eventPublisher);
    }

    @Test
    void rejectsUnknownCenter() {
        when(trainingCenterLookup.findById(centerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command("F-103")))
                .isInstanceOf(TrainingCenterNotFoundException.class);
    }

    @Test
    void rejectsDuplicateFichaNumber() {
        when(fichaRepository.existsByFichaNumber("F-104")).thenReturn(true);

        assertThatThrownBy(() -> service.create(command("F-104")))
                .isInstanceOf(FichaNumberAlreadyExistsException.class);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updatesOnlyOperationalFieldsAndKeepsIdentityAndStatus() {
        Ficha current = ficha("EXECUTION", 30, 0);
        when(fichaRepository.findById(current.getId())).thenReturn(Optional.of(current));

        Ficha updated = service.update(new UpdateFichaUseCase.Command(
                current.getId(),
                "NIGHT", true,
                "HYBRID", true,
                LocalDate.of(2028, 1, 1), true,
                20, true,
                0,
                manageAccess()));

        assertThat(updated.getId()).isEqualTo(current.getId());
        assertThat(updated.getStatus()).isEqualTo("EXECUTION");
        assertThat(updated.getTrainingShift()).isEqualTo("NIGHT");
        assertThat(updated.getTrainingModality()).isEqualTo("HYBRID");
        assertThat(updated.getExpectedEndDate()).isEqualTo(LocalDate.of(2028, 1, 1));
        assertThat(updated.getMaxCapacity()).isEqualTo(20);
        verify(fichaRepository).save(any(Ficha.class));
    }

    @Test
    void rejectsCapacityIncreaseWithoutLearnerLookup() {
        Ficha current = ficha("EXECUTION", 30, 0);
        when(fichaRepository.findById(current.getId())).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.update(new UpdateFichaUseCase.Command(
                current.getId(), null, false, null, false, null, false,
                31, true, 0, manageAccess())))
                .isInstanceOf(FichaCapacityIncreaseNotAllowedException.class);

        verify(fichaRepository, never()).save(any(Ficha.class));
    }

    @Test
    void rejectsTerminalStatuses() {
        for (String status : new String[] {"COMPLETED", "CANCELLED"}) {
            Ficha current = ficha(status, 30, 0);
            when(fichaRepository.findById(current.getId())).thenReturn(Optional.of(current));

            assertThatThrownBy(() -> service.update(new UpdateFichaUseCase.Command(
                    current.getId(), "NIGHT", true, null, false, null, false,
                    null, false, 0, manageAccess())))
                    .isInstanceOf(FichaTerminalStatusException.class);
        }
    }

    @Test
    void rejectsStaleRowVersion() {
        Ficha current = ficha("EXECUTION", 30, 2);
        when(fichaRepository.findById(current.getId())).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.update(new UpdateFichaUseCase.Command(
                current.getId(), "NIGHT", true, null, false, null, false,
                null, false, 1, manageAccess())))
                .isInstanceOf(RowVersionMismatchException.class);

        verify(fichaRepository, never()).save(any(Ficha.class));
    }

    @Test
    void cancelsFichaAndPublishesClosedEvent() {
        Ficha current = ficha("EXECUTION", 30, 0);
        when(fichaRepository.findById(current.getId())).thenReturn(Optional.of(current));

        Ficha cancelled = service.changeStatus(new ChangeFichaStatusUseCase.Command(
                current.getId(), "CANCELLED", LocalDate.of(2026, 12, 20),
                manageAccess(), true));

        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.getActualEndDate()).isEqualTo(LocalDate.of(2026, 12, 20));
        verify(fichaRepository).save(any(Ficha.class));
        verify(eventPublisher).publish(argThat(event ->
                event.event_type().equals("academic.ficha.closed")
                        && event.payload().close_reason().equals("CANCELLED")
                        && event.payload().ficha_id().equals(current.getId())));
    }

    @Test
    void rejectsCancellationWithoutCenterDirectorAndDoesNotPublish() {
        Ficha current = ficha("EXECUTION", 30, 0);
        when(fichaRepository.findById(current.getId())).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.changeStatus(new ChangeFichaStatusUseCase.Command(
                current.getId(), "CANCELLED", null, manageAccess(), false)))
                .isInstanceOf(FichaStatusRoleDeniedException.class);

        verify(fichaRepository, never()).save(any(Ficha.class));
        verify(eventPublisher, never()).publish(any());
    }

    private CreateFichaUseCase.Command command(String fichaNumber) {
        return new CreateFichaUseCase.Command(
                fichaNumber,
                programId,
                centerId,
                LocalDate.now().plusDays(5),
                null,
                "DAY",
                "IN_PERSON",
                20,
                actorId);
    }

    private TrainingProgram program(int version, boolean active) {
        Instant now = Instant.now();
        return TrainingProgram.reconstitute(
                programId,
                UUID.randomUUID(),
                "PROG-01",
                "Program",
                "TECHNOLOGIST",
                1000,
                version,
                active,
                0,
                now,
                actorId,
                now,
                actorId,
                active ? null : now,
                active ? null : actorId);
    }

    private QueryFichasUseCase.AccessContext manageAccess() {
        return new QueryFichasUseCase.AccessContext(
                actorId, null, null, QueryFichasUseCase.ScopeType.GLOBAL);
    }

    private Ficha ficha(String status, int maxCapacity, int rowVersion) {
        Instant now = Instant.now();
        return Ficha.reconstitute(
                UUID.randomUUID(),
                "F-UPDATE-" + UUID.randomUUID().toString().substring(0, 8),
                programId,
                3,
                centerId,
                status,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2028, 1, 1),
                null,
                "DAY",
                "IN_PERSON",
                maxCapacity,
                rowVersion,
                now,
                actorId,
                now,
                actorId);
    }
}
