package co.sena.academic.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Competency aggregate for HU-ACAD-06. */
public final class Competency {

    private final UUID id;
    private final UUID programId;
    private String name;
    private String senaCode;
    private int hours;
    private boolean active;
    private int rowVersion;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant deletedAt;
    private UUID deletedBy;

    private Competency(UUID id,
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
        this.id = requireNonNull(id, "id");
        this.programId = requireNonNull(programId, "programId");
        this.name = requireName(name);
        this.senaCode = requireSenaCode(senaCode);
        this.hours = requireHours(hours);
        this.active = active;
        this.rowVersion = rowVersion;
        this.createdAt = requireNonNull(createdAt, "createdAt");
        this.createdBy = requireNonNull(createdBy, "createdBy");
        this.updatedAt = requireNonNull(updatedAt, "updatedAt");
        this.updatedBy = requireNonNull(updatedBy, "updatedBy");
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
    }

    public static Competency create(UUID programId,
                                    String name,
                                    String senaCode,
                                    int hours,
                                    Boolean active,
                                    UUID actorId) {
        Instant now = Instant.now();
        UUID actor = requireNonNull(actorId, "actorId");
        return new Competency(
                UUID.randomUUID(),
                programId,
                name,
                senaCode,
                hours,
                active == null || active,
                0,
                now,
                actor,
                now,
                actor,
                null,
                null);
    }

    public static Competency reconstitute(UUID id,
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
        return new Competency(
                id, programId, name, senaCode, hours, active, rowVersion,
                createdAt, createdBy, updatedAt, updatedBy, deletedAt, deletedBy);
    }

    public void applyUpdate(String name,
                            String senaCode,
                            Integer hours,
                            UUID actorId) {
        ensureActive();
        if (name != null) {
            this.name = requireName(name);
        }
        if (senaCode != null) {
            this.senaCode = requireSenaCode(senaCode);
        }
        if (hours != null) {
            this.hours = requireHours(hours);
        }
        this.updatedBy = requireNonNull(actorId, "actorId");
        this.updatedAt = Instant.now();
    }

    public void deactivate(UUID actorId) {
        if (!active || deletedAt != null) {
            return;
        }
        UUID actor = requireNonNull(actorId, "actorId");
        Instant now = Instant.now();
        this.active = false;
        this.deletedAt = now;
        this.deletedBy = actor;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    private void ensureActive() {
        if (!active || deletedAt != null) {
            throw new IllegalStateException("A deactivated competency cannot be updated or reactivated");
        }
    }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("name must not exceed 200 characters");
        }
        return value;
    }

    private static String requireSenaCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("senaCode is required");
        }
        if (value.length() > 20) {
            throw new IllegalArgumentException("senaCode must not exceed 20 characters");
        }
        return value;
    }

    private static int requireHours(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("hours must be >= 0");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
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
