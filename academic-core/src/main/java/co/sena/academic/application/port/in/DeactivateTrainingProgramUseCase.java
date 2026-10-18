package co.sena.academic.application.port.in;

import java.util.UUID;

/** HU-ACAD-05: logical deactivation of a training program. */
public interface DeactivateTrainingProgramUseCase {

    void deactivate(Command command);

    record Command(UUID id, int rowVersion, UUID actorId) {
    }
}
