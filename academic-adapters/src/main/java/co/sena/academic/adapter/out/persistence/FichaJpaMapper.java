package co.sena.academic.adapter.out.persistence;

import co.sena.academic.domain.model.Ficha;

final class FichaJpaMapper {

    private FichaJpaMapper() {
    }

    static FichaJpaEntity toEntity(Ficha ficha) {
        return new FichaJpaEntity(
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
                ficha.getCreatedBy(),
                ficha.getUpdatedAt(),
                ficha.getUpdatedBy());
    }

    static Ficha toDomain(FichaJpaEntity entity) {
        return Ficha.reconstitute(
                entity.getId(),
                entity.getFichaNumber(),
                entity.getProgramId(),
                entity.getProgramVersion(),
                entity.getTrainingCenterId(),
                entity.getStatus(),
                entity.getStartDate(),
                entity.getExpectedEndDate(),
                entity.getActualEndDate(),
                entity.getTrainingShift(),
                entity.getTrainingModality(),
                entity.getMaxCapacity(),
                entity.getRowVersion(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy());
    }
}
