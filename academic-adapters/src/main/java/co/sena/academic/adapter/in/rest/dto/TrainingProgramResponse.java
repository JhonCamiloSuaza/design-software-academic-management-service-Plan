package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record TrainingProgramResponse(
        UUID id,
        @JsonProperty("knowledge_network_id") UUID knowledgeNetworkId,
        @JsonProperty("program_code") String programCode,
        String name,
        @JsonProperty("training_level") String trainingLevel,
        @JsonProperty("total_hours") int totalHours,
        int version,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("row_version") int rowVersion,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
