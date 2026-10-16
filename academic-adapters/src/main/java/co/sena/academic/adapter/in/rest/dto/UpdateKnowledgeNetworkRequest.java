package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Body of PUT /knowledge-networks/{id} — matches KnowledgeNetworkUpdate.
 * All fields optional. Null = "no change" for that field.
 */
public record UpdateKnowledgeNetworkRequest(

        @JsonProperty("tech_network_id")
        UUID techNetworkId,

        @Size(min = 1, max = 150, message = "name must not be blank and must not exceed 150 characters")
        String name,

        @Size(min = 1, max = 20, message = "code must not be blank and must not exceed 20 characters")
        String code,

        @JsonProperty("is_active")
        Boolean isActive
) {
}