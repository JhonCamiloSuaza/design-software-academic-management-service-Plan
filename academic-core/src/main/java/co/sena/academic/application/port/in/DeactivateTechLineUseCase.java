package co.sena.academic.application.port.in;

import java.util.UUID;

/**
 * E4 — DELETE /tech-lines/{id}. Sets is_active=false (there is no
 * deleted_at in the real DDL). Requires the same ACADEMIC_PROGRAM_MANAGE —
 * there is no separate "admin" RBAC feature for deactivating tech_line.
 */
public interface DeactivateTechLineUseCase {

    void deactivate(UUID id, UUID actorId);
}