package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.CreateKnowledgeNetworkUseCase;
import co.sena.academic.application.port.in.DeactivateKnowledgeNetworkUseCase;
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

import java.util.List;
import java.util.UUID;

/**
 * HU-ACAD-03. Implements the four knowledge_network input ports.
 * Depends on neither Spring nor JPA — only on the output ports.
 * The @Service/@Transactional annotation is applied in the API module's
 * configuration bean (same convention as TechLineService).
 */
public class KnowledgeNetworkService implements
        CreateKnowledgeNetworkUseCase,
        QueryKnowledgeNetworksUseCase,
        UpdateKnowledgeNetworkUseCase,
        DeactivateKnowledgeNetworkUseCase {

    private final KnowledgeNetworkRepositoryPort repository;
    private final TechNetworkLookupPort techNetworkLookupPort;
    private final TrainingProgramLookupPort trainingProgramLookupPort;

    public KnowledgeNetworkService(KnowledgeNetworkRepositoryPort repository,
                                    TechNetworkLookupPort techNetworkLookupPort,
                                    TrainingProgramLookupPort trainingProgramLookupPort) {
        this.repository = repository;
        this.techNetworkLookupPort = techNetworkLookupPort;
        this.trainingProgramLookupPort = trainingProgramLookupPort;
    }

    // ---- E1: create ----
    @Override
    public KnowledgeNetwork create(CreateKnowledgeNetworkUseCase.Command command) {
        if (!techNetworkLookupPort.existsById(command.techNetworkId())) {
            throw new TechNetworkNotFoundException(command.techNetworkId());
        }
        if (repository.existsByTechNetworkIdAndCode(command.techNetworkId(), command.code())) {
            throw new KnowledgeNetworkCodeAlreadyExistsException(command.techNetworkId(), command.code());
        }
        KnowledgeNetwork knowledgeNetwork =
                KnowledgeNetwork.create(command.techNetworkId(), command.name(), command.code(), command.actorId());
        return repository.save(knowledgeNetwork);
    }

    // ---- E2: query ----
    @Override
    public KnowledgeNetwork getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new KnowledgeNetworkNotFoundException(id));
    }

    @Override
    public Page list(Query query) {
        List<KnowledgeNetwork> content =
                repository.findAll(query.techNetworkId(), query.isActive(), query.page(), query.size(), query.sort());
        long total = repository.count(query.techNetworkId(), query.isActive());
        return new Page(content, total, query.page(), query.size());
    }

    // ---- update ----
    // ---- update ----
    @Override
    public KnowledgeNetwork update(UpdateKnowledgeNetworkUseCase.Command command) {
        KnowledgeNetwork knowledgeNetwork = repository.findById(command.id())
                .orElseThrow(() -> new KnowledgeNetworkNotFoundException(command.id()));

        if (command.techNetworkId() != null
                && !knowledgeNetwork.getTechNetworkId().equals(command.techNetworkId())
                && !techNetworkLookupPort.existsById(command.techNetworkId())) {
            throw new TechNetworkNotFoundException(command.techNetworkId());
        }

        UUID effectiveTechNetworkId = command.techNetworkId() != null
                ? command.techNetworkId() : knowledgeNetwork.getTechNetworkId();
        String effectiveCode = command.code() != null ? command.code() : knowledgeNetwork.getCode();

        if (repository.existsByTechNetworkIdAndCodeAndIdNot(
                effectiveTechNetworkId, effectiveCode, command.id())) {
            throw new KnowledgeNetworkCodeAlreadyExistsException(effectiveTechNetworkId, effectiveCode);
        }

        boolean isDeactivating = knowledgeNetwork.isActive() && Boolean.FALSE.equals(command.isActive());
        if (isDeactivating && trainingProgramLookupPort.existsActiveByKnowledgeNetworkId(command.id())) {
            throw new KnowledgeNetworkHasActiveTrainingProgramsException(command.id());
        }

        knowledgeNetwork.applyUpdate(command.techNetworkId(), command.name(), command.code(),
                command.isActive(), command.actorId());
        return repository.save(knowledgeNetwork);
    }

    // ---- E3: logical deactivation, blocked by active training_program dependents ----
    @Override
    public void deactivate(UUID id, UUID actorId) {
        KnowledgeNetwork knowledgeNetwork = repository.findById(id)
                .orElseThrow(() -> new KnowledgeNetworkNotFoundException(id));

        if (trainingProgramLookupPort.existsActiveByKnowledgeNetworkId(id)) {
            throw new KnowledgeNetworkHasActiveTrainingProgramsException(id);
        }

        knowledgeNetwork.deactivate(actorId);
        repository.save(knowledgeNetwork);
    }
}