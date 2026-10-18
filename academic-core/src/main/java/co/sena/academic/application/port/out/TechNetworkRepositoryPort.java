package co.sena.academic.application.port.out;

import co.sena.academic.domain.model.TechNetwork;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TechNetworkRepositoryPort {

    TechNetwork save(TechNetwork techNetwork);

    Optional<TechNetwork> findById(UUID id);

    boolean existsByTechLineIdAndCode(UUID techLineId, String code);

    /** For E3: when updating, the new code may match its own current code (exclude the current id). */
    boolean existsByTechLineIdAndCodeAndIdNot(UUID techLineId, String code, UUID excludedId);

    /** tech_line_id filter (optional) + pagination (E2). */
    List<TechNetwork> findAll(Optional<UUID> techLineId, int page, int size);

    long count(Optional<UUID> techLineId);
}