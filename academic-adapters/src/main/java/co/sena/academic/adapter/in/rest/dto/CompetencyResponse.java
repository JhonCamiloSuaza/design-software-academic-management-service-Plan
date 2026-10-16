package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record CompetencyResponse(
        UUID id,
        @JsonProperty("program_id") UUID programId,
        String name,
        @JsonProperty("sena_code") String senaCode,
        int hours,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("row_version") int rowVersion,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {
}
