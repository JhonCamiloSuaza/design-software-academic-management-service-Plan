package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.TechNetwork;

import java.util.UUID;

/** E1 — POST /tech-networks. Requires ACADEMIC_PROGRAM_MANAGE in the REST adapter. */
public interface CreateTechNetworkUseCase {

    TechNetwork create(Command command);

    record Command(UUID techLineId, String name, String code, UUID actorId) {
    }
}