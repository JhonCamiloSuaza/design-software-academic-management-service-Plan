package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateFichaRequest(
        @NotBlank(message = "ficha_number is required")
        @Size(max = 20, message = "ficha_number must not exceed 20 characters")
        @JsonProperty("ficha_number")
        String fichaNumber,

        @NotNull(message = "program_id is required")
        @JsonProperty("program_id")
        UUID programId,

        @NotNull(message = "training_center_id is required")
        @JsonProperty("training_center_id")
        UUID trainingCenterId,

        @NotNull(message = "start_date is required")
        @JsonProperty("start_date")
        LocalDate startDate,

        @JsonProperty("expected_end_date")
        LocalDate expectedEndDate,

        @NotNull(message = "training_shift is required")
        @Pattern(regexp = "DAY|NIGHT|MIXED", message = "training_shift must be one of DAY, NIGHT, MIXED")
        @JsonProperty("training_shift")
        String trainingShift,

        @NotNull(message = "training_modality is required")
        @Pattern(regexp = "IN_PERSON|VIRTUAL|HYBRID", message = "training_modality must be one of IN_PERSON, VIRTUAL, HYBRID")
        @JsonProperty("training_modality")
        String trainingModality,

        @NotNull(message = "max_capacity is required")
        @Positive(message = "max_capacity must be >= 1")
        @JsonProperty("max_capacity")
        Integer maxCapacity
) {
}
