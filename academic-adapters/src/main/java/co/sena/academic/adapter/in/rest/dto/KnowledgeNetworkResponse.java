package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Matches EXACTLY the KnowledgeNetwork schema in academic-management.yaml:
 * required: [id, tech_network_id, name, code, is_active, created_at].
 * Deliberately does NOT include created_by/updated_at/updated_by — the
 * contract does not define them (verified against the real OpenAPI, QA point 6).
 */
public record KnowledgeNetworkResponse(
        UUID id,
        @JsonProperty("tech_network_id") UUID techNetworkId,
        String name,
        String code,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("created_at") Instant createdAt
) {
}