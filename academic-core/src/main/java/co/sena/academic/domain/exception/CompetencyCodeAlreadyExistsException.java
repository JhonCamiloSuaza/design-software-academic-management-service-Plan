package co.sena.academic.domain.exception;

import java.util.UUID;

public class CompetencyCodeAlreadyExistsException extends RuntimeException {
    public CompetencyCodeAlreadyExistsException(UUID programId, String senaCode) {
        super("Competency sena_code already exists in program " + programId + ": " + senaCode);
    }
}
