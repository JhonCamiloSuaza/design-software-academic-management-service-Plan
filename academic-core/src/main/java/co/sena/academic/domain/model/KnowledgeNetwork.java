package co.sena.academic.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Knowledge network: third level of the SENA curricular hierarchy
 * (tech_line -> tech_network -> knowledge_network -> training_program).
 * HU-ACAD-03 (RF-ACAD-01, RN-ACAD-01).
 *
 * Pure domain entity: no dependency on JPA, REST, or any framework.
 * Maps 1:1 to the academic_management.knowledge_network table, which
 * does NOT have row_version or deleted_at in the real DDL — deactivation
 * is exclusively logical, via the "active" flag (see DeactivateKnowledgeNetworkUseCase).
 *
 * techNetworkId is immutable once created: the HU does not ask for
 * "moving" a knowledge_network to a different tech_network, only for
 * validating it against a valid parent at creation time (E1).
 */
public final class KnowledgeNetwork {

    private final UUID id;
    private UUID techNetworkId;
    private String name;
    private String code;
    private boolean active;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private KnowledgeNetwork(UUID id, UUID techNetworkId, String name, String code, boolean active,
                              Instant createdAt, UUID createdBy,
                              Instant updatedAt, UUID updatedBy) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.techNetworkId = Objects.requireNonNull(techNetworkId, "techNetworkId must not be null");
        this.name = requireValidName(name);
        this.code = requireValidCode(code);
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null (comes from the JWT)");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.updatedBy = Objects.requireNonNull(updatedBy, "updatedBy must not be null (comes from the JWT)");
    }

    /**
     * Factory to create a brand-new knowledge network (E1).
     * Existence of techNetworkId is validated in the use case (against
     * TechNetworkLookupPort), not here — the domain only enforces
     * that the value is present.
     */
    public static KnowledgeNetwork create(UUID techNetworkId, String name, String code, UUID actorId) {
        Objects.requireNonNull(actorId, "actorId must not be null (comes from the JWT, no SYSTEM actor)");
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        return new KnowledgeNetwork(id, techNetworkId, name, code, true, now, actorId, now, actorId);
    }

    /** Rebuilds the entity from the persistence layer (does not re-validate the actor). */
    public static KnowledgeNetwork reconstitute(UUID id, UUID techNetworkId, String name, String code, boolean active,
                                                 Instant createdAt, UUID createdBy,
                                                 Instant updatedAt, UUID updatedBy) {
        return new KnowledgeNetwork(id, techNetworkId, name, code, active, createdAt, createdBy, updatedAt, updatedBy);
    }

    /**
     * Partial replace (PUT semantics per contract: KnowledgeNetworkUpdate has
     * no required fields). Any null argument means "keep the current value"
     * for that field — only what the client actually sends gets applied.
     * Uniqueness/existence validations live in the use case, not here.
     */
    public void applyUpdate(UUID techNetworkId, String name, String code, Boolean active, UUID actorId) {
        if (techNetworkId != null) {
            this.techNetworkId = techNetworkId;
        }
        if (name != null) {
            this.name = requireValidName(name);
        }
        if (code != null) {
            this.code = requireValidCode(code);
        }
        if (active != null) {
            this.active = active;
        }
        this.updatedBy = Objects.requireNonNull(actorId, "actorId must not be null");
        this.updatedAt = Instant.now();
    }

    /**
     * E3: logical deactivation ("baja"). This table has no deleted_at,
     * only the active flag. Whether it is allowed (no active
     * training_program dependents) is decided in the use case, not here —
     * the domain object cannot see other aggregates.
     */
    public void deactivate(UUID actorId) {
        this.active = false;
        this.updatedBy = Objects.requireNonNull(actorId, "actorId must not be null");
        this.updatedAt = Instant.now();
    }

    private static String requireValidName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (name.length() > 150) {
            throw new IllegalArgumentException("name must not exceed 150 characters (VARCHAR(150) in the DB)");
        }
        return name;
    }

    private static String requireValidCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (code.length() > 20) {
            throw new IllegalArgumentException("code must not exceed 20 characters (VARCHAR(20) in the DB)");
        }
        return code;
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