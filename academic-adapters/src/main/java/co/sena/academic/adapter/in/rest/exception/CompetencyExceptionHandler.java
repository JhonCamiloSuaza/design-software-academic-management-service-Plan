package co.sena.academic.adapter.in.rest.exception;

import co.sena.academic.domain.exception.CompetencyCodeAlreadyExistsException;
import co.sena.academic.domain.exception.CompetencyDeactivatedException;
import co.sena.academic.domain.exception.CompetencyInUseException;
import co.sena.academic.domain.exception.CompetencyNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CompetencyExceptionHandler {

    @ExceptionHandler(CompetencyCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(CompetencyCodeAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_COMPETENCY_CODE_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(CompetencyNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(CompetencyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("ACAD_COMPETENCY_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CompetencyDeactivatedException.class)
    public ResponseEntity<ApiErrorResponse> handleDeactivated(CompetencyDeactivatedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_COMPETENCY_DEACTIVATED", ex.getMessage()));
    }

    @ExceptionHandler(CompetencyInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleInUse(CompetencyInUseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_COMPETENCY_IN_USE", ex.getMessage()));
    }
}
