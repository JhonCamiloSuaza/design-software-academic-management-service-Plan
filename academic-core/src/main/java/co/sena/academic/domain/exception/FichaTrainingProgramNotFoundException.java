package co.sena.academic.domain.exception;

import java.util.UUID;

/** Raised when a ficha references a program that does not exist. */
public class FichaTrainingProgramNotFoundException extends RuntimeException {
    public FichaTrainingProgramNotFoundException(UUID id) {
        super("Training program not found for ficha: " + id);
    }
}
