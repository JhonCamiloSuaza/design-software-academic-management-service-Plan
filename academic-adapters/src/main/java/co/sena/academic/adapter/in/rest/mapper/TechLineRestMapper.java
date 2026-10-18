package co.sena.academic.adapter.in.rest.mapper;

import co.sena.academic.adapter.in.rest.dto.TechLinePageResponse;
import co.sena.academic.adapter.in.rest.dto.TechLineResponse;
import co.sena.academic.application.port.in.QueryTechLinesUseCase;
import co.sena.academic.domain.model.TechLine;
import org.springframework.stereotype.Component;

@Component
public class TechLineRestMapper {

    public TechLineResponse toResponse(TechLine techLine) {
        return new TechLineResponse(
                techLine.getId(),
                techLine.getName(),
                techLine.getCode(),
                techLine.isActive(),
                techLine.getCreatedAt(),
                techLine.getCreatedBy(),
                techLine.getUpdatedAt(),
                techLine.getUpdatedBy()
        );
    }

    public TechLinePageResponse toPageResponse(QueryTechLinesUseCase.Page page) {
        return new TechLinePageResponse(
                page.content().stream().map(this::toResponse).toList(),
                page.totalElements(),
                page.page(),
                page.size()
        );
    }
}