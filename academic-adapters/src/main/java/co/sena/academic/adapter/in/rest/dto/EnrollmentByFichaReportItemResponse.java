package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentByFichaReportItemResponse(
        @JsonProperty("ficha_id") UUID fichaId,
        @JsonProperty("ficha_number") String fichaNumber,
        @JsonProperty("program_id") UUID programId,
        @JsonProperty("program_code") String programCode,
        @JsonProperty("program_name") String programName,
        @JsonProperty("training_center_id") UUID trainingCenterId,
        String status,
        @JsonProperty("max_capacity") int maxCapacity,
        @JsonProperty("training_shift") String trainingShift,
        @JsonProperty("training_modality") String trainingModality,
        @JsonProperty("start_date") LocalDate startDate) {
}
