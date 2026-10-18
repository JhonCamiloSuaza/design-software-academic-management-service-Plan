package co.sena.academic.adapter.out.persistence;

import co.sena.academic.domain.model.TrainingProgram;

final class TrainingProgramJpaMapper {

    private TrainingProgramJpaMapper() {
    }

    static TrainingProgramJpaEntity toEntity(TrainingProgram trainingProgram) {
        return new TrainingProgramJpaEntity(
                trainingProgram.getId(),
                trainingProgram.getKnowledgeNetworkId(),
                trainingProgram.getProgramCode(),
                trainingProgram.getName(),
                trainingProgram.getTrainingLevel(),
                trainingProgram.getTotalHours(),
                trainingProgram.getVersion(),
                trainingProgram.isActive(),
                trainingProgram.getRowVersion(),
                trainingProgram.getCreatedAt(),
                trainingProgram.getCreatedBy(),
                trainingProgram.getUpdatedAt(),
                trainingProgram.getUpdatedBy(),
                trainingProgram.getDeletedAt(),
                trainingProgram.getDeletedBy());
    }

    static TrainingProgram toDomain(TrainingProgramJpaEntity entity) {
        return TrainingProgram.reconstitute(
                entity.getId(),
                entity.getKnowledgeNetworkId(),
                entity.getProgramCode(),
                entity.getName(),
                entity.getTrainingLevel(),
                entity.getTotalHours(),
                entity.getVersion(),
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
