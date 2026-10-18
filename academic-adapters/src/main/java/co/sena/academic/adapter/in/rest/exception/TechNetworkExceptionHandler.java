package co.sena.academic.adapter.in.rest.exception;

import co.sena.academic.domain.exception.TechLineNotActiveException;
import co.sena.academic.domain.exception.TechNetworkCodeAlreadyExistsException;
import co.sena.academic.domain.exception.TechNetworkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * NOTE (DoR-style, needs team confirmation): "ACAD_TECH_NETWORK_CODE_ALREADY_EXISTS"
 * and "ACAD_TECH_LINE_NOT_ACTIVE" are proposed ADDITIVE extensions to
 * error-codes.yaml. TechLineNotFoundException (404, when tech_line_id
 * does not exist at all) is already handled globally by
 * TechLineExceptionHandler — no need to redeclare it here.
 */
@RestControllerAdvice
public class TechNetworkExceptionHandler {

    @ExceptionHandler(TechNetworkCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCodeAlreadyExists(TechNetworkCodeAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_TECH_NETWORK_CODE_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(TechNetworkNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(TechNetworkNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("ACAD_TECH_NETWORK_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(TechLineNotActiveException.class)
    public ResponseEntity<ApiErrorResponse> handleTechLineNotActive(TechLineNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_TECH_LINE_NOT_ACTIVE", ex.getMessage()));
    }
}