package co.sena.academic.domain.exception;

import java.util.UUID;

public class InactiveTrainingCenterException extends RuntimeException {

    public InactiveTrainingCenterException(UUID trainingCenterId) {
        super("Training center is inactive: " + trainingCenterId);
    }
}
