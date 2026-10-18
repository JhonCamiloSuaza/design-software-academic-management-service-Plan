package co.sena.academic.domain.exception;

public class FichaNumberAlreadyExistsException extends RuntimeException {
    public FichaNumberAlreadyExistsException(String fichaNumber) {
        super("Ficha number already exists: " + fichaNumber);
    }
}
