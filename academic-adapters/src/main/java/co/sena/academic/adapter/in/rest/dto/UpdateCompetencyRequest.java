package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCompetencyRequest(
        @Size(max = 200) String name,
        @JsonProperty("sena_code") @Size(max = 20) String senaCode,
        @Min(0) Integer hours,
        @JsonProperty("is_active") Boolean isActive,
        @JsonProperty("row_version") @NotNull @Min(0) Integer rowVersion) {
}
