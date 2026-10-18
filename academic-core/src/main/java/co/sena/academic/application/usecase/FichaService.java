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
import co.sena.academic.domain.exception.FichaCapacityBelowActiveLearnersException;
import co.sena.academic.domain.exception.FichaNotFoundException;
import co.sena.academic.domain.exception.FichaScopeAccessDeniedException;
import co.sena.academic.domain.exception.FichaStartDateMustBeFutureException;
import co.sena.academic.domain.exception.FichaStatusRoleDeniedException;
import co.sena.academic.domain.exception.RowVersionMismatchException;
import co.sena.academic.domain.exception.FichaTrainingProgramNotFoundException;
import co.sena.academic.domain.exception.InactiveTrainingCenterException;
import co.sena.academic.domain.exception.InactiveTrainingProgramException;
import co.sena.academic.domain.exception.TrainingCenterNotFoundException;
import co.sena.academic.domain.model.Ficha;
import co.sena.academic.domain.model.TrainingProgram;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Application service for HU-ACAD-08 ficha registration. */
public class FichaService implements CreateFichaUseCase, QueryFichasUseCase, UpdateFichaUseCase, ChangeFichaStatusUseCase {

    private static final String FICHA_OPENED_EVENT_TYPE = "academic.ficha.opened";
    private static final String FICHA_CLOSED_EVENT_TYPE = "academic.ficha.closed";
    private static final String SOURCE_SERVICE = "academic-management-service";

    private final FichaRepositoryPort fichaRepository;
    private final TrainingProgramRepositoryPort trainingProgramRepository;
    private final TrainingCenterLookupPort trainingCenterLookup;
    private final FichaEventPublisherPort eventPublisher;
    private final FichaOwnershipPort fichaOwnershipPort;

    public FichaService(FichaRepositoryPort fichaRepository,
                        TrainingProgramRepositoryPort trainingProgramRepository,
                        TrainingCenterLookupPort trainingCenterLookup,
                        FichaEventPublisherPort eventPublisher,
                        FichaOwnershipPort fichaOwnershipPort) {
        this.fichaRepository = fichaRepository;
        this.trainingProgramRepository = trainingProgramRepository;
        this.trainingCenterLookup = trainingCenterLookup;
        this.eventPublisher = eventPublisher;
        this.fichaOwnershipPort = fichaOwnershipPort;
    }

    @Override
    public Ficha create(CreateFichaUseCase.Command command) {
        if (!command.startDate().isAfter(LocalDate.now())) {
            throw new FichaStartDateMustBeFutureException(command.startDate());
        }

        TrainingProgram program = trainingProgramRepository.findById(command.programId())
                .orElseThrow(() -> new FichaTrainingProgramNotFoundException(command.programId()));
        if (!program.isActive() || program.getDeletedAt() != null) {
            throw new InactiveTrainingProgramException(command.programId());
        }

        TrainingCenterLookupPort.TrainingCenterReference center = trainingCenterLookup
                .findById(command.trainingCenterId())
                .orElseThrow(() -> new TrainingCenterNotFoundException(command.trainingCenterId()));
        if (!center.active()) {
            throw new InactiveTrainingCenterException(command.trainingCenterId());
        }

        if (fichaRepository.existsByFichaNumber(command.fichaNumber())) {
            throw new FichaNumberAlreadyExistsException(command.fichaNumber());
        }

        Ficha saved = fichaRepository.save(Ficha.create(
                command.fichaNumber(),
                command.programId(),
                program.getVersion(),
                command.trainingCenterId(),
                command.startDate(),
                command.expectedEndDate(),
                command.trainingShift(),
                command.trainingModality(),
                command.maxCapacity(),
                command.actorId()));

        eventPublisher.publish(new FichaEventPublisherPort.EventEnvelope(
                UUID.randomUUID(),
                FICHA_OPENED_EVENT_TYPE,
                1,
                Instant.now(),
                SOURCE_SERVICE,
                FichaEventPublisherPort.FichaPayload.opened(saved, command.actorId()),
                null));

        return saved;
    }

    @Override
    public Ficha getById(UUID id, AccessContext access) {
        ensureReadableScope(access);
        Set<UUID> allowedFichaIds = resolveAllowedFichaIds(access);
        Ficha ficha = fichaRepository.findById(id)
                .orElseThrow(() -> new FichaNotFoundException(id));
        ensureVisible(ficha, access, allowedFichaIds);
        return ficha;
    }

    @Override
    public Page list(Query query) {
        ensureReadableScope(query.access());

        if (query.access().scope() == ScopeType.TRAINING_CENTER
                && query.trainingCenterId().isPresent()
                && !query.trainingCenterId().get().equals(query.access().trainingCenterId())) {
            return new Page(List.of(), 0, query.page(), query.size());
        }

        Optional<UUID> effectiveTrainingCenter = query.access().scope() == ScopeType.TRAINING_CENTER
                ? Optional.of(query.access().trainingCenterId())
                : query.trainingCenterId();
        Optional<Set<UUID>> allowedFichaIds = ownershipFilter(query.access());

        List<Ficha> content = fichaRepository.findAll(
                query.programId(),
                query.status(),
                query.fichaNumber(),
                effectiveTrainingCenter,
                query.from(),
                query.to(),
                allowedFichaIds,
                query.page(),
                query.size(),
                query.sort());
        long total = fichaRepository.count(
                query.programId(),
                query.status(),
                query.fichaNumber(),
                effectiveTrainingCenter,
                query.from(),
                query.to(),
                allowedFichaIds);
        return new Page(content, total, query.page(), query.size());
    }

    @Override
    public Ficha update(UpdateFichaUseCase.Command command) {
        Ficha current = fichaRepository.findById(command.id())
                .orElseThrow(() -> new FichaNotFoundException(command.id()));

        ensureManageScope(command.access(), current);
        current.ensureOperationalDataCanBeUpdated();
        verifyRowVersion(current, command.rowVersion());

        String effectiveShift = command.trainingShiftPresent()
                ? command.trainingShift() : current.getTrainingShift();
        String effectiveModality = command.trainingModalityPresent()
                ? command.trainingModality() : current.getTrainingModality();
        LocalDate effectiveExpectedEndDate = command.expectedEndDatePresent()
                ? command.expectedEndDate() : current.getExpectedEndDate();
        int effectiveCapacity = command.maxCapacityPresent()
                ? command.maxCapacity() : current.getMaxCapacity();

        if (command.maxCapacityPresent() && effectiveCapacity < current.getMaxCapacity()) {
            int activeLearners = fichaOwnershipPort.countActiveLearners(current.getId());
            if (effectiveCapacity < activeLearners) {
                throw new FichaCapacityBelowActiveLearnersException(
                        current.getId(), effectiveCapacity, activeLearners);
            }
        }

        return fichaRepository.save(current.updateOperationalData(
                effectiveShift,
                effectiveModality,
                effectiveExpectedEndDate,
                effectiveCapacity,
                command.access().userId()));
    }

    @Override
    public Ficha changeStatus(ChangeFichaStatusUseCase.Command command) {
        Ficha current = fichaRepository.findById(command.id())
                .orElseThrow(() -> new FichaNotFoundException(command.id()));

        ensureManageScope(command.access(), current);
        if ("CANCELLED".equals(command.status()) && !command.centerDirector()) {
            throw new FichaStatusRoleDeniedException("Only CENTER_DIRECTOR can cancel a ficha");
        }

        Ficha changed = current.transitionStatus(
                command.status(), command.actualEndDate(), command.access().userId());
        Ficha saved = fichaRepository.save(changed);

        if ("COMPLETED".equals(saved.getStatus()) || "CANCELLED".equals(saved.getStatus())) {
            eventPublisher.publish(new FichaEventPublisherPort.EventEnvelope(
                    UUID.randomUUID(),
                    FICHA_CLOSED_EVENT_TYPE,
                    1,
                    Instant.now(),
                    SOURCE_SERVICE,
                    FichaEventPublisherPort.FichaPayload.closed(saved, command.access().userId()),
                    null));
        }
        return saved;
    }

    private void ensureReadableScope(AccessContext access) {
        if (access == null || access.scope() == null) {
            throw new FichaScopeAccessDeniedException("A ficha scope is required");
        }
        if (access.scope() == ScopeType.TRAINING_CENTER && access.trainingCenterId() == null) {
            throw new FichaScopeAccessDeniedException(
                    "training_center_id is required for TRAINING_CENTER");
        }
    }

    private Set<UUID> resolveAllowedFichaIds(AccessContext access) {
        return ownershipFilter(access).orElse(Set.of());
    }

    private void ensureManageScope(AccessContext access, Ficha ficha) {
        if (access == null || access.scope() == null) {
            throw new FichaScopeAccessDeniedException("A ficha manage scope is required");
        }
        if (access.scope() == ScopeType.GLOBAL) {
            return;
        }
        if (access.scope() != ScopeType.TRAINING_CENTER) {
            throw new FichaScopeAccessDeniedException(
                    "Only GLOBAL and TRAINING_CENTER manage scopes are supported");
        }
        if (access.trainingCenterId() == null) {
            throw new FichaScopeAccessDeniedException(
                    "training_center_id is required for TRAINING_CENTER");
        }
        if (!ficha.getTrainingCenterId().equals(access.trainingCenterId())) {
            throw new FichaNotFoundException(ficha.getId());
        }
    }

    private void verifyRowVersion(Ficha ficha, int sentRowVersion) {
        if (ficha.getRowVersion() != sentRowVersion) {
            throw new RowVersionMismatchException(
                    ficha.getId(), sentRowVersion, ficha.getRowVersion());
        }
    }

    private Optional<Set<UUID>> ownershipFilter(AccessContext access) {
        if (access.scope() == ScopeType.OWN_FICHAS) {
            return Optional.of(fichaOwnershipPort.findFichaIdsForInstructor(access.actorId()));
        }
        if (access.scope() == ScopeType.OWN_FICHA_AS_LEARNER) {
            return Optional.of(fichaOwnershipPort.findFichaIdsForLearner(access.actorId()));
        }
        return Optional.empty();
    }

    private void ensureVisible(Ficha ficha, AccessContext access, Set<UUID> allowedFichaIds) {
        if (access.scope() == ScopeType.TRAINING_CENTER
                && !ficha.getTrainingCenterId().equals(access.trainingCenterId())) {
            throw new FichaNotFoundException(ficha.getId());
        }
        if ((access.scope() == ScopeType.OWN_FICHAS
                || access.scope() == ScopeType.OWN_FICHA_AS_LEARNER)
                && !allowedFichaIds.contains(ficha.getId())) {
            throw new FichaNotFoundException(ficha.getId());
        }
    }
}
