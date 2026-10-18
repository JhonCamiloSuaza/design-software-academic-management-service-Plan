package co.sena.academic.adapter.in.rest.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Shared error envelope, per the platform's error-response contract:
 * {@code {"error": {"code", "message", "details", "trace_id"}}}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(ApiError error) {

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(new ApiError(code, message, null, currentTraceId()));
    }

    public static ApiErrorResponse of(String code, String message, List<FieldDetail> details) {
        return new ApiErrorResponse(new ApiError(code, message, details, currentTraceId()));
    }

    /**
        * Read the active trace ID from OTel/Micrometer via MDC (populated automatically by micrometer-tracing-bridge-otel, see academic-management-api/pom.xml + application.yml `management.tracing`).

        * If there is no active span (e.g., slice tests), generate one so that the trace ID never goes to null/blank.
     */
    private static String currentTraceId() {
        String traceId = org.slf4j.MDC.get("traceId");
        return (traceId != null && !traceId.isBlank())
                ? traceId
                : java.util.UUID.randomUUID().toString();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiError(
            String code,
            String message,
            List<FieldDetail> details,
            @JsonProperty("trace_id") String traceId) {
    }

   /** An entry of {@code details} for validation errors (400). */
    public record FieldDetail(String field, String message) {
    }
}