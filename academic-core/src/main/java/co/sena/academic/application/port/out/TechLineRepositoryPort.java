package co.sena.academic.application.port.out;

import co.sena.academic.domain.model.TechLine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: what the domain needs from persistence.
 * The domain does not know there is JPA + Postgres behind it — that is
 * implemented by TechLinePersistenceAdapter in academic-adapters/out/persistence.
 */
public interface TechLineRepositoryPort {

    TechLine save(TechLine techLine);

    Optional<TechLine> findById(UUID id);

    boolean existsByCode(String code);

    /** For E3: when updating, the new code may match its own current code (exclude the current id). */
    boolean existsByCodeAndIdNot(String code, UUID excludedId);

    /** is_active filter + pagination (E2). */
    List<TechLine> findAll(Optional<Boolean> isActive, int page, int size);

    long count(Optional<Boolean> isActive);
}