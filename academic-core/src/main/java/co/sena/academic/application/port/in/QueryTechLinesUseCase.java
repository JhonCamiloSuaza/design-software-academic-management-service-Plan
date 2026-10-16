package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.TechLine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * E2 — GET /tech-lines (paginated, is_active filter) and GET /tech-lines/{id}.
 * Requires ACADEMIC_PROGRAM_VIEW (GLOBAL: SYSTEM_ADMIN, CENTER_DIRECTOR,
 * COORDINATOR, INSTRUCTOR, LEARNER, per the real RBAC seed).
 */
public interface QueryTechLinesUseCase {

    TechLine getById(UUID id);

    Page list(Query query);

    record Query(Optional<Boolean> isActive, int page, int size) {
    }

    record Page(List<TechLine> content, long totalElements, int page, int size) {
    }
}