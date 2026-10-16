package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTrainingProgramRequest(
        @NotNull(message = "knowledge_network_id is required")
        @JsonProperty("knowledge_network_id")
        UUID knowledgeNetworkId,

        @NotBlank(message = "program_code is required")
        @Size(max = 20, message = "program_code must not exceed 20 characters")
        @JsonProperty("program_code")
        String programCode,

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must not exceed 200 characters")
        String name,

        @NotBlank(message = "training_level is required")
        @Pattern(
                regexp = "AUXILIARY|OPERATOR|TECHNICIAN|TECHNOLOGIST",
                message = "training_level must be one of AUXILIARY, OPERATOR, TECHNICIAN, TECHNOLOGIST")
        @JsonProperty("training_level")
        String trainingLevel,

        @NotNull(message = "total_hours is required")
        @PositiveOrZero(message = "total_hours must be >= 0")
        @JsonProperty("total_hours")
        Integer totalHours,

        @JsonProperty("is_active")
        Boolean isActive
) {
}
