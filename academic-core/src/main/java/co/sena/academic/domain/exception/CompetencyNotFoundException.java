package co.sena.academic.domain.exception;

import java.util.UUID;

public class CompetencyNotFoundException extends RuntimeException {
    public CompetencyNotFoundException(UUID id) {
        super("Competency not found: " + id);
    }
}
