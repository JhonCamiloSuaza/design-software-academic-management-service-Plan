package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** DELETE /training-programs/{id} optimistic-concurrency body. */
public record DeactivateTrainingProgramRequest(
        @NotNull(message = "row_version is required")
        @PositiveOrZero(message = "row_version must be >= 0")
        @JsonProperty("row_version")
        Integer rowVersion
) {
}
