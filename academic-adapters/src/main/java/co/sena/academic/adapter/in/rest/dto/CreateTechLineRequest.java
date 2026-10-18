package co.sena.academic.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /tech-lines. Matches TechLineCreate from the contract frozen in shared-contracts. */
public record CreateTechLineRequest(

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must not exceed 150 characters")
        String name,

        @NotBlank(message = "code is required")
        @Size(max = 20, message = "code must not exceed 20 characters")
        String code
) {
}