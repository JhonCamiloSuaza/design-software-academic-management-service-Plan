package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.CreateTechLineUseCase;
import co.sena.academic.application.port.in.DeactivateTechLineUseCase;
import co.sena.academic.application.port.in.QueryTechLinesUseCase;
import co.sena.academic.application.port.in.UpdateTechLineUseCase;
import co.sena.academic.application.port.out.TechLineRepositoryPort;
import co.sena.academic.domain.exception.TechLineCodeAlreadyExistsException;
import co.sena.academic.domain.exception.TechLineNotFoundException;
import co.sena.academic.domain.model.TechLine;

import java.util.List;
import java.util.UUID;

/**
 * HU-ACAD-01. Implements the four tech_line input ports.
 * Depends on neither Spring nor JPA â€” only on the output port.
 * The @Service/@Transactional annotation is applied in the API
 * module's configuration bean (or here, if the team decides to
 * allow Spring annotations in academic-core; in that case add
 * spring-context as a dependency of this module).
 */
public class TechLineService implements
        CreateTechLineUseCase,
        QueryTechLinesUseCase,
        UpdateTechLineUseCase,
        DeactivateTechLineUseCase {

    private final TechLineRepositoryPort repository;

    public TechLineService(TechLineRepositoryPort repository) {
        this.repository = repository;
    }

    // ---- E1: create ----
    @Override
    public TechLine create(CreateTechLineUseCase.Command command) {
        if (repository.existsByCode(command.code())) {
            throw new TechLineCodeAlreadyExistsException(command.code());
        }
        TechLine techLine = TechLine.create(command.name(), command.code(), command.actorId());
        return repository.save(techLine);
    }

    // ---- E2: query ----
    @Override
    public TechLine getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new TechLineNotFoundException(id));
    }

    @Override
    public Page list(Query query) {
        List<TechLine> content = repository.findAll(query.isActive(), query.page(), query.size());
        long total = repository.count(query.isActive());
        return new Page(content, total, query.page(), query.size());
    }

    // ---- E3: update ----
    @Override
    public TechLine update(UpdateTechLineUseCase.Command command) {
        TechLine techLine = repository.findById(command.id())
                .orElseThrow(() -> new TechLineNotFoundException(command.id()));

        if (repository.existsByCodeAndIdNot(command.code(), command.id())) {
            throw new TechLineCodeAlreadyExistsException(command.code());
        }

        techLine.replace(command.name(), command.code(), command.actorId());
        return repository.save(techLine);
    }

    // ---- E4: logical deactivation ----
    @Override
    public void deactivate(UUID id, UUID actorId) {
        TechLine techLine = repository.findById(id)
                .orElseThrow(() -> new TechLineNotFoundException(id));
        techLine.deactivate(actorId);
        repository.save(techLine);
    }
}
