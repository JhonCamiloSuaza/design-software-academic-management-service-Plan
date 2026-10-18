package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.Competency;

import java.util.UUID;

public interface UpdateCompetencyUseCase {
    Competency update(Command command);

    record Command(UUID id,
                   String name,
                   String senaCode,
                   Integer hours,
                   Boolean isActive,
                   int rowVersion,
                   UUID actorId) {
    }
}
