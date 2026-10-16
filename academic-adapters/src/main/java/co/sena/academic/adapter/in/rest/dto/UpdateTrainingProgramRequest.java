package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * PUT /training-programs/{id} body for HU-ACAD-04.
 * row_version is mandatory because it is the optimistic-concurrency token.
 */
public record UpdateTrainingProgramRequest(
        @JsonProperty("knowledge_network_id")
        UUID knowledgeNetworkId,

        @Size(min = 1, max = 20, message = "program_code must not be blank and must not exceed 20 characters")
        @JsonProperty("program_code")
        String programCode,

        @Size(min = 1, max = 200, message = "name must not be blank and must not exceed 200 characters")
        String name,

        @Pattern(
                regexp = "AUXILIARY|OPERATOR|TECHNICIAN|TECHNOLOGIST",
                message = "training_level must be one of AUXILIARY, OPERATOR, TECHNICIAN, TECHNOLOGIST")
        @JsonProperty("training_level")
        String trainingLevel,

        @PositiveOrZero(message = "total_hours must be >= 0")
        @JsonProperty("total_hours")
        Integer totalHours,

        Integer version,

        @JsonProperty("is_active")
        Boolean isActive,

        @NotNull(message = "row_version is required")
        @PositiveOrZero(message = "row_version must be >= 0")
        @JsonProperty("row_version")
        Integer rowVersion
) {
}