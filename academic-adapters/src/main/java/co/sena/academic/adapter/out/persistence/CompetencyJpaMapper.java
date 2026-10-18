package co.sena.academic.adapter.out.persistence;

import co.sena.academic.domain.model.Competency;

public final class CompetencyJpaMapper {
    private CompetencyJpaMapper() {
    }

    public static CompetencyJpaEntity toEntity(Competency competency) {
        return new CompetencyJpaEntity(
                competency.getId(),
                competency.getProgramId(),
                competency.getName(),
                competency.getSenaCode(),
                competency.getHours(),
                competency.isActive(),
                competency.getRowVersion(),
                competency.getCreatedAt(),
                competency.getCreatedBy(),
                competency.getUpdatedAt(),
                competency.getUpdatedBy(),
                competency.getDeletedAt(),
                competency.getDeletedBy());
    }

    public static Competency toDomain(CompetencyJpaEntity entity) {
        return Competency.reconstitute(
                entity.getId(),
                entity.getProgramId(),
                entity.getName(),
                entity.getSenaCode(),
                entity.getHours(),
                entity.isActive(),
                entity.getRowVersion(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                entity.getDeletedAt(),
                entity.getDeletedBy());
    }
}
