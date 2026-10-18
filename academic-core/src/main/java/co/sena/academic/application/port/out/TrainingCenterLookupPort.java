package co.sena.academic.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface TrainingCenterLookupPort {

    Optional<TrainingCenterReference> findById(UUID trainingCenterId);

    record TrainingCenterReference(boolean active) {
    }
}
