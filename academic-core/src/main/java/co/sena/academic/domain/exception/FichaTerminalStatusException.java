package co.sena.academic.domain.exception;

import java.util.UUID;

public class FichaTerminalStatusException extends RuntimeException {

    public FichaTerminalStatusException(UUID id, String status) {
        super("Ficha " + id + " cannot be updated because its status is terminal: " + status);
    }
}
