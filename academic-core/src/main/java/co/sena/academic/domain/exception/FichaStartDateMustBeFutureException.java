package co.sena.academic.domain.exception;

import java.time.LocalDate;

public class FichaStartDateMustBeFutureException extends RuntimeException {

    public FichaStartDateMustBeFutureException(LocalDate startDate) {
        super("start_date must be in the future: " + startDate);
    }
}
