package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.TechNetwork;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** E2 — GET /tech-networks (tech_line_id filter, paginated) and GET /tech-networks/{id}. Requires ACADEMIC_PROGRAM_VIEW. */
public interface QueryTechNetworksUseCase {

    TechNetwork getById(UUID id);

    Page list(Query query);

    record Query(Optional<UUID> techLineId, int page, int size) {
    }

    record Page(List<TechNetwork> content, long totalElements, int page, int size) {
    }
}