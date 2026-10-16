package co.sena.academic.adapter.in.rest.mapper;

import co.sena.academic.adapter.in.rest.dto.TrainingProgramPageResponse;
import co.sena.academic.adapter.in.rest.dto.TrainingProgramResponse;
import co.sena.academic.application.port.in.QueryTrainingProgramsUseCase;
import co.sena.academic.domain.model.TrainingProgram;
import org.springframework.stereotype.Component;

@Component
public class TrainingProgramRestMapper {

    public TrainingProgramResponse toResponse(TrainingProgram trainingProgram) {
        return new TrainingProgramResponse(
                trainingProgram.getId(),
                trainingProgram.getKnowledgeNetworkId(),
                trainingProgram.getProgramCode(),
                trainingProgram.getName(),
                trainingProgram.getTrainingLevel(),
                trainingProgram.getTotalHours(),
                trainingProgram.getVersion(),
                trainingProgram.isActive(),
                trainingProgram.getRowVersion(),
                trainingProgram.getCreatedAt(),
                trainingProgram.getUpdatedAt());
    }

    public TrainingProgramPageResponse toPageResponse(QueryTrainingProgramsUseCase.Page page) {
        int totalPages = page.size() == 0
                ? 0
                : (int) Math.ceil((double) page.totalElements() / page.size());
        return new TrainingProgramPageResponse(
                page.content().stream().map(this::toResponse).toList(),
                new TrainingProgramPageResponse.Pagination(
                        page.page() + 1,
                        page.size(),
                        page.totalElements(),
                        totalPages));
    }
}
