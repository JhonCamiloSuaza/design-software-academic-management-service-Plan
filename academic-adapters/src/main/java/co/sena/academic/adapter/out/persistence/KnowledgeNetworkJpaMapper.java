package co.sena.academic.adapter.out.persistence;

import co.sena.academic.domain.model.KnowledgeNetwork;

final class KnowledgeNetworkJpaMapper {

    private KnowledgeNetworkJpaMapper() {
    }

    static KnowledgeNetworkJpaEntity toEntity(KnowledgeNetwork domain) {
        return new KnowledgeNetworkJpaEntity(
                domain.getId(),
                domain.getTechNetworkId(),
                domain.getName(),
                domain.getCode(),
                domain.isActive(),
                domain.getCreatedAt(),
                domain.getCreatedBy(),
                domain.getUpdatedAt(),
                domain.getUpdatedBy()
        );
    }

    static KnowledgeNetwork toDomain(KnowledgeNetworkJpaEntity entity) {
        return KnowledgeNetwork.reconstitute(
                entity.getId(),
                entity.getTechNetworkId(),
                entity.getName(),
                entity.getCode(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }
}