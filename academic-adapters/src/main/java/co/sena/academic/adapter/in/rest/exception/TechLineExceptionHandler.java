package co.sena.academic.adapter.in.rest.exception;

import co.sena.academic.domain.exception.TechLineCodeAlreadyExistsException;
import co.sena.academic.domain.exception.TechLineNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;
import java.util.List;

/**
* IMPORTANT (HU's DoR): "ACAD_TECH_LINE_CODE_ALREADY_EXISTS" is a

* proposed ADDITIVE extension to error-codes.yaml. Confirm with the Contracts team

before closing the HU. The same applies to COMMON_VALIDATION_ERROR

* / COMMON_FORBIDDEN if they do not already exist in the shared catalog.

* All bodies use the shared envelope {@link ApiErrorResponse}:

* {@code {"error": {"code", "message", "details", "trace_id"}}}.
 */
@RestControllerAdvice
public class TechLineExceptionHandler {

    @ExceptionHandler(TechLineCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCodeAlreadyExists(TechLineCodeAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_TECH_LINE_CODE_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(TechLineNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(TechLineNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("ACAD_TECH_LINE_NOT_FOUND", ex.getMessage()));
    }

    /** Bean Validation fails on @Valid (400). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiErrorResponse.FieldDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldDetail(fe.getField(), messageOf(fe)))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("COMMON_VALIDATION_ERROR", "Request validation failed", details));
    }

    /** Denials of @PreAuthorize / method-security (403), e.g., RBAC. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of("COMMON_FORBIDDEN", "You do not have permission to perform this action"));
    }

    private static String messageOf(FieldError fieldError) {
        return fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Invalid value";
    }

    /** Bean Validation fails on loose @RequestParam (e.g. page, page_size) (400). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ApiErrorResponse.FieldDetail> details = ex.getConstraintViolations().stream()
                .map(cv -> new ApiErrorResponse.FieldDetail(
                        lastNode(cv.getPropertyPath().toString()), cv.getMessage()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("COMMON_VALIDATION_ERROR", "Request validation failed", details));
    }

        private static String lastNode(String propertyPath) {
        int idx = propertyPath.lastIndexOf('.');
        return idx >= 0 ? propertyPath.substring(idx + 1) : propertyPath;
    }

    /** Invariantes de dominio violadas que no pasaron por Bean Validation (400, no 500). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("COMMON_VALIDATION_ERROR", ex.getMessage()));
    }
}