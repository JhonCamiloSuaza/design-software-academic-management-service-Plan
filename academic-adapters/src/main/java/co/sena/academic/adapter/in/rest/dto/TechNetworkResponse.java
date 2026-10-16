package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record TechNetworkResponse(
        UUID id,
        @JsonProperty("tech_line_id") UUID techLineId,
        String name,
        String code,
        boolean active,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
}