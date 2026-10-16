package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.CreateTrainingProgramUseCase;
import co.sena.academic.application.port.in.DeactivateTrainingProgramUseCase;
import co.sena.academic.application.port.in.QueryTrainingProgramsUseCase;
import co.sena.academic.application.port.in.UpdateTrainingProgramUseCase;
import co.sena.academic.application.port.out.KnowledgeNetworkRepositoryPort;
import co.sena.academic.application.port.out.TrainingProgramEventPublisherPort;
import co.sena.academic.application.port.out.TrainingProgramRepositoryPort;
import co.sena.academic.domain.exception.KnowledgeNetworkNotFoundException;
import co.sena.academic.domain.exception.RowVersionMismatchException;
import co.sena.academic.domain.exception.TrainingProgramCodeAlreadyExistsException;
import co.sena.academic.domain.exception.TrainingProgramNotFoundException;
import co.sena.academic.domain.model.TrainingProgram;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

/** Application service for HU-ACAD-04 and HU-ACAD-05 training program behavior. */
public class TrainingProgramService implements
        CreateTrainingProgramUseCase,
        UpdateTrainingProgramUseCase,
        DeactivateTrainingProgramUseCase,
        QueryTrainingProgramsUseCase {

    private final TrainingProgramRepositoryPort repository;
    private final KnowledgeNetworkRepositoryPort knowledgeNetworkRepository;
    private final TrainingProgramEventPublisherPort eventPublisher;

    public TrainingProgramService(TrainingProgramRepositoryPort repository,
                                  KnowledgeNetworkRepositoryPort knowledgeNetworkRepository,
                                  TrainingProgramEventPublisherPort eventPublisher) {
        this.repository = repository;
        this.knowledgeNetworkRepository = knowledgeNetworkRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public TrainingProgram create(CreateTrainingProgramUseCase.Command command) {
        ensureKnowledgeNetworkExists(command.knowledgeNetworkId());
        if (repository.existsByProgramCode(command.programCode())) {
            throw new TrainingProgramCodeAlreadyExistsException(command.programCode());
        }

        TrainingProgram trainingProgram = TrainingProgram.create(
                command.knowledgeNetworkId(),
                command.programCode(),
                command.name(),
                command.trainingLevel(),
                command.totalHours(),
                command.isActive(),
                command.actorId());

        TrainingProgram saved = repository.save(trainingProgram);
        eventPublisher.publish(new TrainingProgramEventPublisherPort.EventEnvelope(
                UUID.randomUUID(),
                "academic.program.created",
                1,
                Instant.now(),
                "academic-management-service",
                TrainingProgramEventPublisherPort.ProgramPayload.from(saved, command.actorId()),
                null));
        return saved;
    }

    @Override
    public TrainingProgram update(UpdateTrainingProgramUseCase.Command command) {
        TrainingProgram trainingProgram = repository.findById(command.id())
                .orElseThrow(() -> new TrainingProgramNotFoundException(command.id()));

        verifyRowVersion(trainingProgram, command.rowVersion());

        if (command.knowledgeNetworkId() != null
                && !trainingProgram.getKnowledgeNetworkId().equals(command.knowledgeNetworkId())) {
            ensureKnowledgeNetworkExists(command.knowledgeNetworkId());
        }

        String effectiveProgramCode = command.programCode() != null
                ? command.programCode()
                : trainingProgram.getProgramCode();
        if (repository.existsByProgramCodeAndIdNot(effectiveProgramCode, command.id())) {
            throw new TrainingProgramCodeAlreadyExistsException(effectiveProgramCode);
        }

        trainingProgram.applyUpdate(
                command.knowledgeNetworkId(),
                command.programCode(),
                command.name(),
                command.trainingLevel(),
                command.totalHours(),
                command.version(),
                command.isActive(),
                command.actorId());

        return repository.save(trainingProgram);
    }

    @Override
    public void deactivate(DeactivateTrainingProgramUseCase.Command command) {
        TrainingProgram trainingProgram = repository.findById(command.id())
                .orElseThrow(() -> new TrainingProgramNotFoundException(command.id()));

        // DELETE is idempotent after the same resource has already been soft-deleted.
        if (trainingProgram.getDeletedAt() != null) {
            return;
        }

        verifyRowVersion(trainingProgram, command.rowVersion());

        trainingProgram.deactivate(command.actorId());
        repository.save(trainingProgram);
    }

    @Override
    public TrainingProgram getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new TrainingProgramNotFoundException(id));
    }

    @Override
    public Page list(Query query) {
        List<TrainingProgram> content = repository.findAll(
                query.knowledgeNetworkId(),
                query.programCode(),
                query.trainingLevel(),
                query.isActive(),
                query.page(),
                query.size(),
                query.sort());
        long total = repository.count(
                query.knowledgeNetworkId(),
                query.programCode(),
                query.trainingLevel(),
                query.isActive());
        return new Page(content, total, query.page(), query.size());
    }

    private void verifyRowVersion(TrainingProgram trainingProgram, int sentRowVersion) {
        if (trainingProgram.getRowVersion() != sentRowVersion) {
            throw new RowVersionMismatchException(
                    trainingProgram.getId(), sentRowVersion, trainingProgram.getRowVersion());
        }
    }

    private void ensureKnowledgeNetworkExists(UUID knowledgeNetworkId) {
        if (knowledgeNetworkRepository.findById(knowledgeNetworkId).isEmpty()) {
            throw new KnowledgeNetworkNotFoundException(knowledgeNetworkId);
        }
    }
}
