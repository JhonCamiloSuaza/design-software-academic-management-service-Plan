package co.sena.academic.domain.exception;

import java.util.UUID;

public class FichaNotFoundException extends RuntimeException {

    public FichaNotFoundException(UUID id) {
        super("Ficha not found: " + id);
    }
}
