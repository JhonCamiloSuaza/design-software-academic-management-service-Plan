package co.sena.academic.domain.exception;

import java.util.UUID;

public class CompetencyDeactivatedException extends RuntimeException {
    public CompetencyDeactivatedException(UUID id) {
        super("Competency is deactivated and cannot be updated or reactivated: " + id);
    }
}
