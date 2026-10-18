package co.sena.academic.adapter.in.rest.exception;

import co.sena.academic.domain.exception.FichaNumberAlreadyExistsException;
import co.sena.academic.domain.exception.FichaCapacityIncreaseNotAllowedException;
import co.sena.academic.domain.exception.FichaCapacityBelowActiveLearnersException;
import co.sena.academic.domain.exception.FichaNotFoundException;
import co.sena.academic.domain.exception.FichaOwnershipUnavailableException;
import co.sena.academic.domain.exception.FichaScopeAccessDeniedException;
import co.sena.academic.domain.exception.FichaStartDateMustBeFutureException;
import co.sena.academic.domain.exception.FichaTerminalStatusException;
import co.sena.academic.domain.exception.FichaInvalidStatusTransitionException;
import co.sena.academic.domain.exception.FichaStatusRoleDeniedException;
import co.sena.academic.domain.exception.FichaTrainingProgramNotFoundException;
import co.sena.academic.domain.exception.InactiveTrainingCenterException;
import co.sena.academic.domain.exception.InactiveTrainingProgramException;
import co.sena.academic.domain.exception.ReferenceDataServiceUnavailableException;
import co.sena.academic.domain.exception.RowVersionMismatchException;
import co.sena.academic.domain.exception.TrainingCenterNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FichaExceptionHandler {

    @ExceptionHandler(FichaNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleFichaNotFound(FichaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("ACAD_FICHA_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(FichaScopeAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleScopeAccessDenied(
            FichaScopeAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of("ACAD_FICHA_SCOPE_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(FichaOwnershipUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleOwnershipUnavailable(
            FichaOwnershipUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of("ACAD_FICHA_OWNERSHIP_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(FichaTrainingProgramNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleFichaTrainingProgramNotFound(
            FichaTrainingProgramNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of("ACAD_TRAINING_PROGRAM_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InactiveTrainingProgramException.class)
    public ResponseEntity<ApiErrorResponse> handleInactiveTrainingProgram(
            InactiveTrainingProgramException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of("ACAD_TRAINING_PROGRAM_INACTIVE", ex.getMessage()));
    }

    @ExceptionHandler(FichaNumberAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleFichaNumberAlreadyExists(
            FichaNumberAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_FICHA_NUMBER_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(FichaStartDateMustBeFutureException.class)
    public ResponseEntity<ApiErrorResponse> handleStartDateMustBeFuture(
            FichaStartDateMustBeFutureException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of("ACAD_FICHA_START_DATE_NOT_FUTURE", ex.getMessage()));
    }

    @ExceptionHandler(TrainingCenterNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTrainingCenterNotFound(
            TrainingCenterNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of("ACAD_TRAINING_CENTER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InactiveTrainingCenterException.class)
    public ResponseEntity<ApiErrorResponse> handleInactiveTrainingCenter(
            InactiveTrainingCenterException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of("ACAD_TRAINING_CENTER_INACTIVE", ex.getMessage()));
    }

    @ExceptionHandler(ReferenceDataServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleReferenceDataUnavailable(
            ReferenceDataServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of("ACAD_REFERENCE_DATA_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(FichaTerminalStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleTerminalStatus(FichaTerminalStatusException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_TERMINAL_STATUS", ex.getMessage()));
    }

    @ExceptionHandler(FichaInvalidStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStatusTransition(FichaInvalidStatusTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_INVALID_STATUS_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(FichaStatusRoleDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleStatusRoleDenied(FichaStatusRoleDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of("ACAD_FICHA_STATUS_ROLE_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(FichaCapacityIncreaseNotAllowedException.class)
    public ResponseEntity<ApiErrorResponse> handleCapacityIncrease(
            FichaCapacityIncreaseNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of("ACAD_FICHA_CAPACITY_INCREASE_NOT_ALLOWED", ex.getMessage()));
    }

    @ExceptionHandler(FichaCapacityBelowActiveLearnersException.class)
    public ResponseEntity<ApiErrorResponse> handleCapacityBelowActiveLearners(
            FichaCapacityBelowActiveLearnersException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of(
                        "ACAD_FICHA_CAPACITY_BELOW_ACTIVE_LEARNERS", ex.getMessage()));
    }

    @ExceptionHandler(RowVersionMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleRowVersionMismatch(RowVersionMismatchException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ROW_VERSION_MISMATCH", ex.getMessage()));
    }
}
