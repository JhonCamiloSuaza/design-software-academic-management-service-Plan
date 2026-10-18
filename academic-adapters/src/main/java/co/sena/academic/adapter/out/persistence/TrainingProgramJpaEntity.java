package co.sena.academic.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "academic_management", name = "training_program")
public class TrainingProgramJpaEntity {

    @Id
    private UUID id;

    @Column(name = "knowledge_network_id", nullable = false)
    private UUID knowledgeNetworkId;

    @Column(name = "program_code", nullable = false, length = 20)
    private String programCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "training_level", nullable = false, length = 20)
    private String trainingLevel;

    @Column(name = "total_hours", nullable = false)
    private int totalHours;

    @Column(nullable = false)
    private int version;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Version
    @Column(name = "row_version", nullable = false)
    private int rowVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    protected TrainingProgramJpaEntity() {
    }

    TrainingProgramJpaEntity(UUID id,
                             UUID knowledgeNetworkId,
                             String programCode,
                             String name,
                             String trainingLevel,
                             int totalHours,
                             int version,
                             boolean active,
                             int rowVersion,
                             Instant createdAt,
                             UUID createdBy,
                             Instant updatedAt,
                             UUID updatedBy,
                             Instant deletedAt,
                             UUID deletedBy) {
        this.id = id;
        this.knowledgeNetworkId = knowledgeNetworkId;
        this.programCode = programCode;
        this.name = name;
        this.trainingLevel = trainingLevel;
        this.totalHours = totalHours;
        this.version = version;
        this.active = active;
        this.rowVersion = rowVersion;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
    }

    public UUID getId() { return id; }
    public UUID getKnowledgeNetworkId() { return knowledgeNetworkId; }
    public String getProgramCode() { return programCode; }
    public String getName() { return name; }
    public String getTrainingLevel() { return trainingLevel; }
    public int getTotalHours() { return totalHours; }
    public int getVersion() { return version; }
    public boolean isActive() { return active; }
    public int getRowVersion() { return rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public Instant getDeletedAt() { return deletedAt; }
    public UUID getDeletedBy() { return deletedBy; }
}
