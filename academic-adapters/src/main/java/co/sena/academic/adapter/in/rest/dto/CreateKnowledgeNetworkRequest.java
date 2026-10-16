package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Body of POST /knowledge-networks. Matches KnowledgeNetworkCreate from
 * the contract frozen in shared-contracts. Field naming follows the same
 * convention as the sibling CreateTechNetworkRequest (snake_case for the
 * parent-reference field via @JsonProperty, e.g. "tech_line_id" there /
 * "tech_network_id" here) — only plain camelCase fields (name, code) skip
 * @JsonProperty, same as "is_active" on GET /tech-lines uses snake_case
 * for its own query param.
 */
public record CreateKnowledgeNetworkRequest(

        @NotNull(message = "tech_network_id is required")
        @JsonProperty("tech_network_id")
        UUID techNetworkId,

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must not exceed 150 characters")
        String name,

        @NotBlank(message = "code is required")
        @Size(max = 20, message = "code must not exceed 20 characters")
        String code
) {
}