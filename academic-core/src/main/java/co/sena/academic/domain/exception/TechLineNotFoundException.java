package co.sena.academic.domain.exception;

import java.util.UUID;

/**
 * Thrown when looking up/updating/deactivating a technical line whose
 * id does not exist. The REST adapter translates this to 404 (E3, E4).
 */
public class TechLineNotFoundException extends RuntimeException {

    public TechLineNotFoundException(UUID id) {
        super("No tech_line found with id=" + id);
    }
}