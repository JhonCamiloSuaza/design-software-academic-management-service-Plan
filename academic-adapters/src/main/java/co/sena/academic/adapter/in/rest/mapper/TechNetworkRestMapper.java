package co.sena.academic.adapter.in.rest.mapper;

import co.sena.academic.adapter.in.rest.dto.TechNetworkPageResponse;
import co.sena.academic.adapter.in.rest.dto.TechNetworkResponse;
import co.sena.academic.application.port.in.QueryTechNetworksUseCase;
import co.sena.academic.domain.model.TechNetwork;
import org.springframework.stereotype.Component;

@Component
public class TechNetworkRestMapper {

    public TechNetworkResponse toResponse(TechNetwork techNetwork) {
        return new TechNetworkResponse(
                techNetwork.getId(),
                techNetwork.getTechLineId(),
                techNetwork.getName(),
                techNetwork.getCode(),
                techNetwork.isActive(),
                techNetwork.getCreatedAt(),
                techNetwork.getCreatedBy(),
                techNetwork.getUpdatedAt(),
                techNetwork.getUpdatedBy()
        );
    }

    public TechNetworkPageResponse toPageResponse(QueryTechNetworksUseCase.Page page) {
        return new TechNetworkPageResponse(
                page.content().stream().map(this::toResponse).toList(),
                page.totalElements(),
                page.page(),
                page.size()
        );
    }
}