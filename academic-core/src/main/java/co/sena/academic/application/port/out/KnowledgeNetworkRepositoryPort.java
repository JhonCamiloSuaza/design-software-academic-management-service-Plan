package co.sena.academic.application.port.out;

import co.sena.academic.domain.model.KnowledgeNetwork;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeNetworkRepositoryPort {

    KnowledgeNetwork save(KnowledgeNetwork knowledgeNetwork);

    Optional<KnowledgeNetwork> findById(UUID id);

    boolean existsByTechNetworkIdAndCode(UUID techNetworkId, String code);

    boolean existsByTechNetworkIdAndCodeAndIdNot(UUID techNetworkId, String code, UUID excludedId);

    /** tech_network_id/is_active filters (optional) + pagination + sort (E2, per OpenAPI contract). */
    List<KnowledgeNetwork> findAll(Optional<UUID> techNetworkId, Optional<Boolean> isActive,
                                    int page, int size, Optional<String> sort);

    long count(Optional<UUID> techNetworkId, Optional<Boolean> isActive);
}