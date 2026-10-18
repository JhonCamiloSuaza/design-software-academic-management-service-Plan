package co.sena.academic.adapter.in.rest.exception;

import co.sena.academic.domain.exception.RowVersionMismatchException;
import co.sena.academic.domain.exception.TrainingProgramCodeAlreadyExistsException;
import co.sena.academic.domain.exception.TrainingProgramNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TrainingProgramExceptionHandler {

    @ExceptionHandler(TrainingProgramCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleProgramCodeAlreadyExists(
            TrainingProgramCodeAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_PROGRAM_CODE_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(TrainingProgramNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(TrainingProgramNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("ACAD_TRAINING_PROGRAM_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RowVersionMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleRowVersionMismatch(RowVersionMismatchException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ROW_VERSION_MISMATCH", ex.getMessage()));
    }
}
