package co.sena.academic.domain.exception;

public class FichaOwnershipUnavailableException extends RuntimeException {

    public FichaOwnershipUnavailableException(String scope) {
        super("Ownership source is not configured for scope " + scope);
    }

    public FichaOwnershipUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
