package co.sena.academic.domain.exception;

import java.util.UUID;

public class CompetencyInUseException extends RuntimeException {
    public CompetencyInUseException(UUID id) {
        super("Competency cannot be modified because it is associated with learning outcomes used by fichas: " + id);
    }
}
