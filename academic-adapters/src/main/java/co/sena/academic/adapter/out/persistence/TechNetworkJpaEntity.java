package co.sena.academic.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps 1:1 to academic_management.tech_network (real DDL:
 * 01_ddl/03_tables/002_create_table_tech_network.sql). No row_version.
 * tech_line_id is stored as a plain UUID column (no JPA @ManyToOne
 * relation) — same convention as the rest of this hexagonal codebase.
 */
@Entity
@Table(
        schema = "academic_management",
        name = "tech_network",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_tech_network_tech_line_id_code",
                columnNames = {"tech_line_id", "code"})
)
public class TechNetworkJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tech_line_id", nullable = false)
    private UUID techLineId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    protected TechNetworkJpaEntity() {
        // required by JPA
    }

    public TechNetworkJpaEntity(UUID id, UUID techLineId, String name, String code, boolean active,
                                 Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy) {
        this.id = id;
        this.techLineId = techLineId;
        this.name = name;
        this.code = code;
        this.active = active;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public UUID getTechLineId() { return techLineId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}