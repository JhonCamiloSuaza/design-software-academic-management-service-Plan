package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FichaResponse(
        UUID id,
        @JsonProperty("ficha_number") String fichaNumber,
        @JsonProperty("program_id") UUID programId,
        @JsonProperty("program_version") int programVersion,
        @JsonProperty("training_center_id") UUID trainingCenterId,
        String status,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("expected_end_date") LocalDate expectedEndDate,
        @JsonProperty("actual_end_date") LocalDate actualEndDate,
        @JsonProperty("training_shift") String trainingShift,
        @JsonProperty("training_modality") String trainingModality,
        @JsonProperty("max_capacity") int maxCapacity,
        @JsonProperty("row_version") int rowVersion,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
