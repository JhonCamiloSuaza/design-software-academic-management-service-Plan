package co.sena.academic.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps 1:1 to academic_management.tech_line as defined in the real DDL
 * (design-software-academic-management-db, 01_ddl/03_tables/001_create_table_tech_line.sql):
 *
 *   id UUID PK, name VARCHAR(150), code VARCHAR(20), is_active BOOLEAN,
 *   created_at TIMESTAMPTZ, created_by UUID, updated_at TIMESTAMPTZ, updated_by UUID.
 *
 * Deliberately has NO row_version or deleted_at: this table does not
 * have them either. uq_tech_line_code is declared as a unique constraint
 * on the "code" column (the real constraint lives in Liquibase — it is
 * mirrored here only so Hibernate does not attempt to create a different
 * schema if ddl-auto=validate is ever used).
 */
@Entity
@Table(
        schema = "academic_management",
        name = "tech_line",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(name = "uq_tech_line_code", columnNames = "code")
)
public class TechLineJpaEntity {

    @Id
    private UUID id;

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

    protected TechLineJpaEntity() {
        // required by JPA
    }

    public TechLineJpaEntity(UUID id, String name, String code, boolean active,
                              Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.active = active;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}