package co.sena.academic.adapter.in.rest.mapper;

import co.sena.academic.adapter.in.rest.dto.KnowledgeNetworkPageResponse;
import co.sena.academic.adapter.in.rest.dto.KnowledgeNetworkResponse;
import co.sena.academic.application.port.in.QueryKnowledgeNetworksUseCase;
import co.sena.academic.domain.model.KnowledgeNetwork;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeNetworkRestMapper {
    public KnowledgeNetworkResponse toResponse(KnowledgeNetwork knowledgeNetwork) {
        return new KnowledgeNetworkResponse(
                knowledgeNetwork.getId(),
                knowledgeNetwork.getTechNetworkId(),
                knowledgeNetwork.getName(),
                knowledgeNetwork.getCode(),
                knowledgeNetwork.isActive(),
                knowledgeNetwork.getCreatedAt()
        );
    }

    public KnowledgeNetworkPageResponse toPageResponse(QueryKnowledgeNetworksUseCase.Page page) {
        int totalPages = page.size() == 0 ? 0 : (int) Math.ceil((double) page.totalElements() / page.size());
        return new KnowledgeNetworkPageResponse(
                page.content().stream().map(this::toResponse).toList(),
                new KnowledgeNetworkPageResponse.Pagination(
                        page.page() + 1,   // 0-based interno -> 1-based hacia el contrato
                        page.size(),
                        page.totalElements(),
                        totalPages
                )
        );
    }
}