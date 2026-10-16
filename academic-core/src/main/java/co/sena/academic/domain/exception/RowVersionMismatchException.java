package co.sena.academic.domain.exception;

import java.util.UUID;

public class RowVersionMismatchException extends RuntimeException {
    public RowVersionMismatchException(UUID id, int expectedRowVersion, int currentRowVersion) {
        super("row_version mismatch for resource " + id
                + ": expected " + expectedRowVersion + " but current is " + currentRowVersion);
    }

    public RowVersionMismatchException(UUID id) {
        super("row_version mismatch for resource " + id);
    }
}
