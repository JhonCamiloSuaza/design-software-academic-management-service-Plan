package co.sena.academic.adapter.in.rest.mapper;

import co.sena.academic.adapter.in.rest.dto.CompetencyPageResponse;
import co.sena.academic.adapter.in.rest.dto.CompetencyResponse;
import co.sena.academic.application.port.in.QueryCompetenciesUseCase;
import co.sena.academic.domain.model.Competency;
import org.springframework.stereotype.Component;

@Component
public class CompetencyRestMapper {

    public CompetencyResponse toResponse(Competency competency) {
        return new CompetencyResponse(
                competency.getId(),
                competency.getProgramId(),
                competency.getName(),
                competency.getSenaCode(),
                competency.getHours(),
                competency.isActive(),
                competency.getRowVersion(),
                competency.getCreatedAt(),
                competency.getUpdatedAt());
    }

    public CompetencyPageResponse toPageResponse(QueryCompetenciesUseCase.Page page) {
        int totalPages = page.size() == 0
                ? 0
                : (int) Math.ceil((double) page.totalElements() / page.size());
        return new CompetencyPageResponse(
                page.content().stream().map(this::toResponse).toList(),
                new CompetencyPageResponse.Pagination(
                        page.page() + 1,
                        page.size(),
                        page.totalElements(),
                        totalPages));
    }
}
