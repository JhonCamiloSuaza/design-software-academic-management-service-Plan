package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Body of POST /tech-networks. Matches TechNetworkCreate from shared-contracts. */
public record CreateTechNetworkRequest(

        @NotNull(message = "tech_line_id is required")
        @JsonProperty("tech_line_id")
        UUID techLineId,

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must not exceed 150 characters")
        String name,

        @NotBlank(message = "code is required")
        @Size(max = 20, message = "code must not exceed 20 characters")
        String code
) {
}