package co.sena.academic.adapter.in.rest.exception;

import co.sena.academic.domain.exception.KnowledgeNetworkCodeAlreadyExistsException;
import co.sena.academic.domain.exception.KnowledgeNetworkHasActiveTrainingProgramsException;
import co.sena.academic.domain.exception.KnowledgeNetworkNotFoundException;
import co.sena.academic.domain.exception.TechNetworkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * IMPORTANT (HU's DoR): "ACAD_KNOWLEDGE_NETWORK_CODE_ALREADY_EXISTS",
 * "ACAD_KNOWLEDGE_NETWORK_NOT_FOUND", "ACAD_TECH_NETWORK_NOT_FOUND" and
 * "ACAD_KNOWLEDGE_NETWORK_HAS_ACTIVE_TRAINING_PROGRAMS" are proposed
 * ADDITIVE extensions to error-codes.yaml. Confirm with the Contracts
 * team before closing the HU.
 *
 * This class intentionally does NOT redeclare handlers for
 * MethodArgumentNotValidException / AccessDeniedException: those are
 * already registered globally by TechLineExceptionHandler and Spring
 * would throw "Ambiguous @ExceptionHandler method mapped" at startup if
 * two @RestControllerAdvice beans handled the exact same exception type.
 * Only the knowledge_network-specific exceptions (plus the
 * missing-required-query-param case, unique to this endpoint) live here.
 *
 * All bodies use the shared envelope {@link ApiErrorResponse}:
 * {@code {"error": {"code", "message", "details", "trace_id"}}}.
 */
@RestControllerAdvice
public class KnowledgeNetworkExceptionHandler {

    @ExceptionHandler(KnowledgeNetworkCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCodeAlreadyExists(KnowledgeNetworkCodeAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_KNOWLEDGE_NETWORK_CODE_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(KnowledgeNetworkNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(KnowledgeNetworkNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("ACAD_KNOWLEDGE_NETWORK_NOT_FOUND", ex.getMessage()));
    }

    /** E1: tech_network_id in the body does not reference an existing tech_network. */
    @ExceptionHandler(TechNetworkNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTechNetworkNotFound(TechNetworkNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("ACAD_TECH_NETWORK_NOT_FOUND", ex.getMessage()));
    }

    /** E3: blocked by training_program dependents with is_active=true. */
    @ExceptionHandler(KnowledgeNetworkHasActiveTrainingProgramsException.class)
    public ResponseEntity<ApiErrorResponse> handleActiveDependents(KnowledgeNetworkHasActiveTrainingProgramsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ACAD_KNOWLEDGE_NETWORK_HAS_ACTIVE_TRAINING_PROGRAMS", ex.getMessage()));
    }

    /** E2: GET /knowledge-networks without the required tech_network_id query param. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("COMMON_VALIDATION_ERROR", ex.getMessage()));
    }
}