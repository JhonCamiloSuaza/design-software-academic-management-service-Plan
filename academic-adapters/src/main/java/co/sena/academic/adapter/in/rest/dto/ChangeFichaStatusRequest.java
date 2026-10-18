package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record ChangeFichaStatusRequest(
        @NotNull
        @Pattern(regexp = "INDUCTION|EXECUTION|PRODUCTIVE_STAGE|COMPLETED|CANCELLED")
        String status,
        @JsonProperty("actual_end_date")
        LocalDate actualEndDate) {
}
