package co.sena.academic.adapter.out.persistence;

import co.sena.academic.domain.model.TechNetwork;

final class TechNetworkJpaMapper {

    private TechNetworkJpaMapper() {
    }

    static TechNetworkJpaEntity toEntity(TechNetwork domain) {
        return new TechNetworkJpaEntity(
                domain.getId(),
                domain.getTechLineId(),
                domain.getName(),
                domain.getCode(),
                domain.isActive(),
                domain.getCreatedAt(),
                domain.getCreatedBy(),
                domain.getUpdatedAt(),
                domain.getUpdatedBy()
        );
    }

    static TechNetwork toDomain(TechNetworkJpaEntity entity) {
        return TechNetwork.reconstitute(
                entity.getId(),
                entity.getTechLineId(),
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