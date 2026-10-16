package co.sena.academic.domain.exception;

import java.util.UUID;

public class TrainingCenterNotFoundException extends RuntimeException {

    public TrainingCenterNotFoundException(UUID trainingCenterId) {
        super("Training center not found: " + trainingCenterId);
    }
}
