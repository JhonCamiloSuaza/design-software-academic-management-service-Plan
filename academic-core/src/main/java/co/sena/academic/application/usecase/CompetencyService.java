package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.CreateCompetencyUseCase;
import co.sena.academic.application.port.in.DeactivateCompetencyUseCase;
import co.sena.academic.application.port.in.QueryCompetenciesUseCase;
import co.sena.academic.application.port.in.UpdateCompetencyUseCase;
import co.sena.academic.application.port.out.CompetencyEventPublisherPort;
import co.sena.academic.application.port.out.CompetencyRepositoryPort;
import co.sena.academic.application.port.out.CompetencyUsagePort;
import co.sena.academic.application.port.out.TrainingProgramRepositoryPort;
import co.sena.academic.domain.exception.CompetencyCodeAlreadyExistsException;
import co.sena.academic.domain.exception.CompetencyDeactivatedException;
import co.sena.academic.domain.exception.CompetencyInUseException;
import co.sena.academic.domain.exception.CompetencyNotFoundException;
import co.sena.academic.domain.exception.InactiveTrainingProgramException;
import co.sena.academic.domain.exception.RowVersionMismatchException;
import co.sena.academic.domain.exception.TrainingProgramNotFoundException;
import co.sena.academic.domain.model.Competency;
import co.sena.academic.domain.model.TrainingProgram;

import java.time.Instant;
import java.util.UUID;

/** Application service for HU-ACAD-06. */
public class CompetencyService implements
        CreateCompetencyUseCase,
        UpdateCompetencyUseCase,
        DeactivateCompetencyUseCase,
        QueryCompetenciesUseCase {

    private static final String EVENT_TYPE = "academic.competency.updated";
    private static final String SOURCE_SERVICE = "academic-management-service";

    private final CompetencyRepositoryPort repository;
    private final TrainingProgramRepositoryPort trainingProgramRepository;
    private final CompetencyUsagePort usagePort;
    private final CompetencyEventPublisherPort eventPublisher;

    public CompetencyService(CompetencyRepositoryPort repository,
                             TrainingProgramRepositoryPort trainingProgramRepository,
                             CompetencyUsagePort usagePort,
                             CompetencyEventPublisherPort eventPublisher) {
        this.repository = repository;
        this.trainingProgramRepository = trainingProgramRepository;
        this.usagePort = usagePort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Competency create(CreateCompetencyUseCase.Command command) {
        ensureProgramActive(command.programId());
        if (repository.existsByProgramIdAndSenaCode(command.programId(), command.senaCode())) {
            throw new CompetencyCodeAlreadyExistsException(command.programId(), command.senaCode());
        }

        Competency saved = repository.save(Competency.create(
                command.programId(),
                command.name(),
                command.senaCode(),
                command.hours(),
                command.isActive(),
                command.actorId()));
        publish(saved, command.actorId(), "CREATED");
        return saved;
    }

    @Override
    public Competency update(UpdateCompetencyUseCase.Command command) {
        Competency competency = getRequired(command.id());
        ensureProgramActive(competency.getProgramId());
        ensureMutable(competency);
        verifyRowVersion(competency, command.rowVersion());
        ensureNotInUse(competency);

        String effectiveCode = command.senaCode() == null ? competency.getSenaCode() : command.senaCode();
        if (repository.existsByProgramIdAndSenaCodeAndIdNot(
                competency.getProgramId(), effectiveCode, competency.getId())) {
            throw new CompetencyCodeAlreadyExistsException(competency.getProgramId(), effectiveCode);
        }

        if (Boolean.FALSE.equals(command.isActive())) {
            competency.deactivate(command.actorId());
            Competency saved = repository.save(competency);
            publish(saved, command.actorId(), "DEACTIVATED");
            return saved;
        }

        competency.applyUpdate(command.name(), command.senaCode(), command.hours(), command.actorId());
        Competency saved = repository.save(competency);
        publish(saved, command.actorId(), "UPDATED");
        return saved;
    }

    @Override
    public void deactivate(DeactivateCompetencyUseCase.Command command) {
        Competency competency = getRequired(command.id());
        if (!competency.isActive() || competency.getDeletedAt() != null) {
            return;
        }
        ensureProgramActive(competency.getProgramId());
        ensureNotInUse(competency);
        competency.deactivate(command.actorId());
        Competency saved = repository.save(competency);
        publish(saved, command.actorId(), "DEACTIVATED");
    }

    @Override
    public Competency getById(UUID id) {
        return getRequired(id);
    }

    @Override
    public Page list(Query query) {
        ensureProgramExists(query.programId());
        var content = repository.findAll(
                query.programId(), query.isActive(), query.page(), query.size(), query.sort());
        long total = repository.count(query.programId(), query.isActive());
        return new Page(content, total, query.page(), query.size());
    }

    private Competency getRequired(UUID id) {
        return repository.findById(id).orElseThrow(() -> new CompetencyNotFoundException(id));
    }

    private TrainingProgram getProgramRequired(UUID programId) {
        return trainingProgramRepository.findById(programId)
                .orElseThrow(() -> new TrainingProgramNotFoundException(programId));
    }

    private void ensureProgramExists(UUID programId) {
        getProgramRequired(programId);
    }

    private void ensureProgramActive(UUID programId) {
        TrainingProgram program = getProgramRequired(programId);
        if (!program.isActive() || program.getDeletedAt() != null) {
            throw new InactiveTrainingProgramException(programId);
        }
    }

    private void ensureMutable(Competency competency) {
        if (!competency.isActive() || competency.getDeletedAt() != null) {
            throw new CompetencyDeactivatedException(competency.getId());
        }
    }

    private void ensureNotInUse(Competency competency) {
        if (usagePort.isUsedByOpenFichas(competency.getId(), competency.getProgramId())) {
            throw new CompetencyInUseException(competency.getId());
        }
    }

    private void verifyRowVersion(Competency competency, int sentRowVersion) {
        if (competency.getRowVersion() != sentRowVersion) {
            throw new RowVersionMismatchException(
                    competency.getId(), sentRowVersion, competency.getRowVersion());
        }
    }

    private void publish(Competency competency, UUID actorId, String changeType) {
        eventPublisher.publish(new CompetencyEventPublisherPort.EventEnvelope(
                UUID.randomUUID(),
                EVENT_TYPE,
                1,
                Instant.now(),
                SOURCE_SERVICE,
                CompetencyEventPublisherPort.CompetencyPayload.from(competency, actorId, changeType),
                null));
    }
}
