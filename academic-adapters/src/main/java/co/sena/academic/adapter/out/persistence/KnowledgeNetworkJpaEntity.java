package co.sena.academic.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps 1:1 to academic_management.knowledge_network as defined in the
 * real DDL (design-software-academic-management-db):
 *
 *   id UUID PK, tech_network_id UUID (FK RESTRICT, fk_knowledge_network_tech_network),
 *   name VARCHAR(150), code VARCHAR(20), is_active BOOLEAN,
 *   created_at TIMESTAMPTZ, created_by UUID, updated_at TIMESTAMPTZ, updated_by UUID.
 *
 * Deliberately has NO row_version or deleted_at, same convention as
 * tech_line. uq_knowledge_network_tech_network_id_code is a composite
 * unique constraint on (tech_network_id, code) — mirrored here only so
 * Hibernate does not attempt to create a different schema if
 * ddl-auto=validate is ever used; the real constraint lives in Liquibase.
 */
@Entity
@Table(
        schema = "academic_management",
        name = "knowledge_network",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(
                name = "uq_knowledge_network_tech_network_id_code",
                columnNames = {"tech_network_id", "code"})
)
public class KnowledgeNetworkJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tech_network_id", nullable = false)
    private UUID techNetworkId;

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

    protected KnowledgeNetworkJpaEntity() {
        // required by JPA
    }

    public KnowledgeNetworkJpaEntity(UUID id, UUID techNetworkId, String name, String code, boolean active,
                                      Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy) {
        this.id = id;
        this.techNetworkId = techNetworkId;
        this.name = name;
        this.code = code;
        this.active = active;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public UUID getId() { return id; }
    public UUID getTechNetworkId() { return techNetworkId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}