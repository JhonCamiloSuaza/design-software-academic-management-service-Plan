package co.sena.academic.adapter.out.persistence;

import co.sena.academic.domain.model.TechLine;

final class TechLineJpaMapper {

    private TechLineJpaMapper() {
    }

    static TechLineJpaEntity toEntity(TechLine domain) {
        return new TechLineJpaEntity(
                domain.getId(),
                domain.getName(),
                domain.getCode(),
                domain.isActive(),
                domain.getCreatedAt(),
                domain.getCreatedBy(),
                domain.getUpdatedAt(),
                domain.getUpdatedBy()
        );
    }

    static TechLine toDomain(TechLineJpaEntity entity) {
        return TechLine.reconstitute(
                entity.getId(),
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