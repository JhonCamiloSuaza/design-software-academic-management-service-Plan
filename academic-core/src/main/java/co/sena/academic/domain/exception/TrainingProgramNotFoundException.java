package co.sena.academic.domain.exception;

import java.util.UUID;

public class TrainingProgramNotFoundException extends RuntimeException {
    public TrainingProgramNotFoundException(UUID id) {
        super("Training program not found: " + id);
    }
}
