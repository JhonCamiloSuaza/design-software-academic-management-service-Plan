package co.sena.academic.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Technical network: second level of the SENA curricular hierarchy,
 * belongs to a tech_line. HU-ACAD-02 (RF-ACAD-01, RN-ACAD-01).
 *
 * Pure domain entity: no dependency on JPA, REST, or any framework.
 * Maps 1:1 to academic_management.tech_network, which does NOT have
 * row_version or deleted_at — deactivation is exclusively logical.
 *
 * techLineId is immutable after creation: this HU does not support
 * moving a tech_network to a different tech_line.
 */
public final class TechNetwork {

    private final UUID id;
    private final UUID techLineId;
    private String name;
    private String code;
    private boolean active;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private TechNetwork(UUID id, UUID techLineId, String name, String code, boolean active,
                         Instant createdAt, UUID createdBy,
                         Instant updatedAt, UUID updatedBy) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.techLineId = Objects.requireNonNull(techLineId, "techLineId must not be null");
        this.name = requireValidName(name);
        this.code = requireValidCode(code);
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null (comes from the JWT)");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.updatedBy = Objects.requireNonNull(updatedBy, "updatedBy must not be null (comes from the JWT)");
    }

    /** Factory to create a brand-new technical network (E1). Existence/active checks on techLineId happen in the use case, not here. */
    public static TechNetwork create(UUID techLineId, String name, String code, UUID actorId) {
        Objects.requireNonNull(actorId, "actorId must not be null (comes from the JWT, no SYSTEM actor)");
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        return new TechNetwork(id, techLineId, name, code, true, now, actorId, now, actorId);
    }

    /** Rebuilds the entity from the persistence layer (does not re-validate the actor). */
    public static TechNetwork reconstitute(UUID id, UUID techLineId, String name, String code, boolean active,
                                            Instant createdAt, UUID createdBy,
                                            Instant updatedAt, UUID updatedBy) {
        return new TechNetwork(id, techLineId, name, code, active, createdAt, createdBy, updatedAt, updatedBy);
    }

    /** E3: full replace of name/code (uniqueness is validated in the use case, not here). techLineId is NOT changed. */
    public void replace(String name, String code, UUID actorId) {
        this.name = requireValidName(name);
        this.code = requireValidCode(code);
        this.updatedBy = Objects.requireNonNull(actorId, "actorId must not be null");
        this.updatedAt = Instant.now();
    }

    /** E3: logical deactivation. This table has no deleted_at, only the active flag. */
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
    public UUID getTechLineId() { return techLineId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}