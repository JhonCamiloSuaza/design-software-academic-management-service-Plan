package co.sena.academic.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Read-only projection used by the HU-ACAD-06 E3 usage guard. */
@Entity
@Table(schema = "academic_management", name = "learning_outcome")
public class LearningOutcomeUsageJpaEntity {
    @Id
    private UUID id;

    @Column(name = "competency_id", nullable = false)
    private UUID competencyId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected LearningOutcomeUsageJpaEntity() {
    }
}
