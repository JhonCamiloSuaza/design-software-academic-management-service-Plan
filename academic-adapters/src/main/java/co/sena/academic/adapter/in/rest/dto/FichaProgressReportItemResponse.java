package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.UUID;

public record FichaProgressReportItemResponse(
        @JsonProperty("ficha_id") UUID fichaId,
        @JsonProperty("ficha_number") String fichaNumber,
        String status,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("expected_end_date") LocalDate expectedEndDate,
        @JsonProperty("actual_end_date") LocalDate actualEndDate,
        @JsonProperty("days_elapsed") int daysElapsed,
        @JsonProperty("days_remaining") Integer daysRemaining,
        @JsonProperty("is_delayed") boolean delayed) {
}
