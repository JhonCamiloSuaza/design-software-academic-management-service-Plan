package co.sena.academic.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of PUT /tech-networks/{id} — full replace of name/code. tech_line_id is not reassignable. */
public record UpdateTechNetworkRequest(

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must not exceed 150 characters")
        String name,

        @NotBlank(message = "code is required")
        @Size(max = 20, message = "code must not exceed 20 characters")
        String code
) {
}