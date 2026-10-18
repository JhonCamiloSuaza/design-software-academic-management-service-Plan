package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.Competency;

import java.util.UUID;

public interface CreateCompetencyUseCase {
    Competency create(Command command);

    record Command(UUID programId,
                   String name,
                   String senaCode,
                   int hours,
                   Boolean isActive,
                   UUID actorId) {
    }
}
