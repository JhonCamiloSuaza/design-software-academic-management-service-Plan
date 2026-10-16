package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.TechNetwork;

import java.util.UUID;

/** E3 — PUT /tech-networks/{id}. Requires ACADEMIC_PROGRAM_MANAGE. 404 if it does not exist. techLineId is not reassignable. */
public interface UpdateTechNetworkUseCase {

    TechNetwork update(Command command);

    record Command(UUID id, String name, String code, UUID actorId) {
    }
}