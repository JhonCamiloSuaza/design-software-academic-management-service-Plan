package co.sena.academic.domain.exception;

import java.util.UUID;

public class ReferenceDataServiceUnavailableException extends RuntimeException {

    public ReferenceDataServiceUnavailableException(UUID trainingCenterId, Throwable cause) {
        super("Reference data service is unavailable while validating training center: " + trainingCenterId, cause);
    }
}
