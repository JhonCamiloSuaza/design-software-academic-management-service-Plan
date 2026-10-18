package co.sena.academic.domain.exception;

import java.util.UUID;

public class FichaInvalidStatusTransitionException extends RuntimeException {

    public FichaInvalidStatusTransitionException(UUID fichaId, String currentStatus, String targetStatus) {
        super("Invalid ficha status transition for " + fichaId + ": "
                + currentStatus + " -> " + targetStatus);
    }
}
