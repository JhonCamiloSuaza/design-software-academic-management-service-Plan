package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCompetencyRequest(
        @NotBlank @Size(max = 200) String name,
        @JsonProperty("sena_code") @NotBlank @Size(max = 20) String senaCode,
        @NotNull @Min(0) Integer hours,
        @JsonProperty("is_active") Boolean isActive) {
}
