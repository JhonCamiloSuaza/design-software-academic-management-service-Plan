package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.CreateTechNetworkUseCase;
import co.sena.academic.application.port.in.DeactivateTechNetworkUseCase;
import co.sena.academic.application.port.in.QueryTechNetworksUseCase;
import co.sena.academic.application.port.in.UpdateTechNetworkUseCase;
import co.sena.academic.application.port.out.TechLineLookupPort;
import co.sena.academic.application.port.out.TechNetworkRepositoryPort;
import co.sena.academic.domain.exception.TechLineNotActiveException;
import co.sena.academic.domain.exception.TechLineNotFoundException;
import co.sena.academic.domain.exception.TechNetworkCodeAlreadyExistsException;
import co.sena.academic.domain.exception.TechNetworkNotFoundException;
import co.sena.academic.domain.model.TechNetwork;

import java.util.List;
import java.util.UUID;

/**
 * HU-ACAD-02. Implements the four tech_network input ports.
 * Depends on neither Spring nor JPA — only on the output ports.
 */
public class TechNetworkService implements
        CreateTechNetworkUseCase,
        QueryTechNetworksUseCase,
        UpdateTechNetworkUseCase,
        DeactivateTechNetworkUseCase {

    private final TechNetworkRepositoryPort repository;
    private final TechLineLookupPort techLineLookupPort;

    public TechNetworkService(TechNetworkRepositoryPort repository, TechLineLookupPort techLineLookupPort) {
        this.repository = repository;
        this.techLineLookupPort = techLineLookupPort;
    }

    // ---- E1: create ----
    @Override
    public TechNetwork create(CreateTechNetworkUseCase.Command command) {
        if (!techLineLookupPort.existsById(command.techLineId())) {
            throw new TechLineNotFoundException(command.techLineId());
        }
        if (!techLineLookupPort.isActiveById(command.techLineId())) {
            throw new TechLineNotActiveException(command.techLineId());
        }
        if (repository.existsByTechLineIdAndCode(command.techLineId(), command.code())) {
            throw new TechNetworkCodeAlreadyExistsException(command.techLineId(), command.code());
        }
        TechNetwork techNetwork = TechNetwork.create(
                command.techLineId(), command.name(), command.code(), command.actorId());
        return repository.save(techNetwork);
    }

    // ---- E2: query ----
    @Override
    public TechNetwork getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new TechNetworkNotFoundException(id));
    }

    @Override
    public Page list(Query query) {
        List<TechNetwork> content = repository.findAll(query.techLineId(), query.page(), query.size());
        long total = repository.count(query.techLineId());
        return new Page(content, total, query.page(), query.size());
    }

    // ---- E3: update ----
    @Override
    public TechNetwork update(UpdateTechNetworkUseCase.Command command) {
        TechNetwork techNetwork = repository.findById(command.id())
                .orElseThrow(() -> new TechNetworkNotFoundException(command.id()));

        if (repository.existsByTechLineIdAndCodeAndIdNot(
                techNetwork.getTechLineId(), command.code(), command.id())) {
            throw new TechNetworkCodeAlreadyExistsException(techNetwork.getTechLineId(), command.code());
        }

        techNetwork.replace(command.name(), command.code(), command.actorId());
        return repository.save(techNetwork);
    }

    // ---- E3: logical deactivation ----
    @Override
    public void deactivate(UUID id, UUID actorId) {
        TechNetwork techNetwork = repository.findById(id)
                .orElseThrow(() -> new TechNetworkNotFoundException(id));
        techNetwork.deactivate(actorId);
        repository.save(techNetwork);
    }
}