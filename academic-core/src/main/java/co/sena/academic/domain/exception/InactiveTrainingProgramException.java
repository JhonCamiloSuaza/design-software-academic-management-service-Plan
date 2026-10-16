package co.sena.academic.domain.exception;

import java.util.UUID;

public class InactiveTrainingProgramException extends RuntimeException {
    public InactiveTrainingProgramException(UUID programId) {
        super("Training program " + programId + " is inactive and cannot be used for this operation");
    }
}
