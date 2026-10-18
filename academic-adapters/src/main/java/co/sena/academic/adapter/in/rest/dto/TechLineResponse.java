package co.sena.academic.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

/** Matches the TechLine schema already frozen in shared-contracts@develop. */
public record TechLineResponse(
        UUID id,
        String name,
        String code,
        boolean active,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
}