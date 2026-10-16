package co.sena.academic.application.port.in;

import java.util.UUID;

/** E3 — DELETE /tech-networks/{id}. Sets is_active=false. Requires ACADEMIC_PROGRAM_MANAGE. */
public interface DeactivateTechNetworkUseCase {

    void deactivate(UUID id, UUID actorId);
}