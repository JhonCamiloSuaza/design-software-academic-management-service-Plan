package co.sena.academic.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "academic_management", name = "competency")
public class CompetencyJpaEntity {

    @Id
    private UUID id;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "sena_code", nullable = false, length = 20)
    private String senaCode;

    @Column(nullable = false)
    private int hours;

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

    protected CompetencyJpaEntity() {
    }

    public CompetencyJpaEntity(UUID id,
                               UUID programId,
                               String name,
                               String senaCode,
                               int hours,
                               boolean active,
                               int rowVersion,
                               Instant createdAt,
                               UUID createdBy,
                               Instant updatedAt,
                               UUID updatedBy,
                               Instant deletedAt,
                               UUID deletedBy) {
        this.id = id;
        this.programId = programId;
        this.name = name;
        this.senaCode = senaCode;
        this.hours = hours;
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
    public UUID getProgramId() { return programId; }
    public String getName() { return name; }
    public String getSenaCode() { return senaCode; }
    public int getHours() { return hours; }
    public boolean isActive() { return active; }
    public int getRowVersion() { return rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public Instant getDeletedAt() { return deletedAt; }
    public UUID getDeletedBy() { return deletedBy; }
}
