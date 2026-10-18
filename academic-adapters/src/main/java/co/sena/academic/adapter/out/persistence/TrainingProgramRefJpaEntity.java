package co.sena.academic.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Read-only "shadow" mapping of academic_management.training_program —
 * one level below knowledge_network, owned by a sibling/future HU. This
 * service does NOT implement training_program's CRUD; it only needs to
 * check, on E3 (deactivation), whether active dependents exist for a
 * given knowledge_network_id, via TrainingProgramLookupPort /
 * TrainingProgramLookupPersistenceAdapter.
 *
 * Only the columns needed for that existence check are mapped on
 * purpose. If training_program already has its own HU/entity in this
 * service by the time this is merged, delete this class and reuse that
 * one's repository instead.
 */
@Entity
@Table(schema = "academic_management", name = "training_program")
public class TrainingProgramRefJpaEntity {

    @Id
    private UUID id;

    @Column(name = "knowledge_network_id", nullable = false)
    private UUID knowledgeNetworkId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected TrainingProgramRefJpaEntity() {
        // required by JPA
    }
}