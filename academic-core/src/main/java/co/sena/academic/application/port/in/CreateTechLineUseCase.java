package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.TechLine;

import java.util.UUID;

/** E1 — POST /tech-lines. Requires ACADEMIC_PROGRAM_MANAGE in the REST adapter. */
public interface CreateTechLineUseCase {

    TechLine create(Command command);

    record Command(String name, String code, UUID actorId) {
    }
}