package co.sena.academic.application.port.out;

import java.util.Set;
import java.util.UUID;

/** External ownership boundary used to resolve OWN ficha scopes. */
public interface FichaOwnershipPort {

    Set<UUID> findFichaIdsForInstructor(UUID instructorId);

    Set<UUID> findFichaIdsForLearner(UUID learnerId);

    int countActiveLearners(UUID fichaId);
}
