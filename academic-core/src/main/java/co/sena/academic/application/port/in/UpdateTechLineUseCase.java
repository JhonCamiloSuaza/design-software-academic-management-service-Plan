package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.TechLine;

import java.util.UUID;

/** E3 — PUT /tech-lines/{id}. Requires ACADEMIC_PROGRAM_MANAGE. 404 if it does not exist. */
public interface UpdateTechLineUseCase {

    TechLine update(Command command);

    record Command(UUID id, String name, String code, UUID actorId) {
    }
}