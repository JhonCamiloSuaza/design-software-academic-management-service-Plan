package co.sena.academic.adapter.in.rest.mapper;

import co.sena.academic.adapter.in.rest.dto.FichaResponse;
import co.sena.academic.adapter.in.rest.dto.FichaPageResponse;
import co.sena.academic.application.port.in.QueryFichasUseCase;
import co.sena.academic.domain.model.Ficha;
import org.springframework.stereotype.Component;

@Component
public class FichaRestMapper {

    public FichaResponse toResponse(Ficha ficha) {
        return new FichaResponse(
                ficha.getId(),
                ficha.getFichaNumber(),
                ficha.getProgramId(),
                ficha.getProgramVersion(),
                ficha.getTrainingCenterId(),
                ficha.getStatus(),
                ficha.getStartDate(),
                ficha.getExpectedEndDate(),
                ficha.getActualEndDate(),
                ficha.getTrainingShift(),
                ficha.getTrainingModality(),
                ficha.getMaxCapacity(),
                ficha.getRowVersion(),
                ficha.getCreatedAt(),
                ficha.getUpdatedAt());
    }

    public FichaPageResponse toPageResponse(QueryFichasUseCase.Page page) {
        int totalPages = page.size() == 0
                ? 0
                : (int) Math.ceil((double) page.totalElements() / page.size());
        return new FichaPageResponse(
                page.content().stream().map(this::toResponse).toList(),
                new FichaPageResponse.Pagination(
                        page.page() + 1,
                        page.size(),
                        page.totalElements(),
                        totalPages));
    }
}
