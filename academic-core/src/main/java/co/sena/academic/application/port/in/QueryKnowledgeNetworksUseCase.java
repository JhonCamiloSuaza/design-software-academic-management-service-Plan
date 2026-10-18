package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.KnowledgeNetwork;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * E2 — GET /knowledge-networks (paginated, optional tech_network_id/is_active
 * filters, optional sort) and GET /knowledge-networks/{id}.
 * tech_network_id is OPTIONAL here (query filter) per academic-management.yaml
 * — but remains REQUIRED in KnowledgeNetworkCreate (POST body), do not confuse the two.
 */
public interface QueryKnowledgeNetworksUseCase {

    KnowledgeNetwork getById(UUID id);

    Page list(Query query);

    record Query(Optional<UUID> techNetworkId, Optional<Boolean> isActive, int page, int size, Optional<String> sort) {
        public Query(UUID techNetworkId, Optional<Boolean> isActive, int page, int size, String sort) {
            this(Optional.ofNullable(techNetworkId), isActive, page, size, Optional.ofNullable(sort));
        }
    }

    record Page(List<KnowledgeNetwork> content, long totalElements, int page, int size) {
    }
}