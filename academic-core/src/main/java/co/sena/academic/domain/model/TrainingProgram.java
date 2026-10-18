package co.sena.academic.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Training program aggregate used by HU-ACAD-04 and HU-ACAD-05. */
public final class TrainingProgram {

    private static final Set<String> ALLOWED_TRAINING_LEVELS = Set.of(
            "AUXILIARY", "OPERATOR", "TECHNICIAN", "TECHNOLOGIST");

    private final UUID id;
    private UUID knowledgeNetworkId;
    private String programCode;
    private String name;
    private String trainingLevel;
    private int totalHours;
    private int version;
    private boolean active;
    private int rowVersion;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant deletedAt;
    private UUID deletedBy;

    private TrainingProgram(UUID id,
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
        this.id = requireNonNull(id, "id");
        this.knowledgeNetworkId = requireNonNull(knowledgeNetworkId, "knowledgeNetworkId");
        this.programCode = requireProgramCode(programCode);
        this.name = requireName(name);
        this.trainingLevel = requireTrainingLevel(trainingLevel);
        this.totalHours = requireTotalHours(totalHours);
        this.version = version;
        this.active = active;
        this.rowVersion = rowVersion;
        this.createdAt = requireNonNull(createdAt, "createdAt");
        this.createdBy = requireNonNull(createdBy, "createdBy");
        this.updatedAt = requireNonNull(updatedAt, "updatedAt");
        this.updatedBy = requireNonNull(updatedBy, "updatedBy");
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
    }

    public static TrainingProgram create(UUID knowledgeNetworkId,
                                         String programCode,
                                         String name,
                                         String trainingLevel,
                                         int totalHours,
                                         Boolean active,
                                         UUID actorId) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        return new TrainingProgram(
                id,
                knowledgeNetworkId,
                programCode,
                name,
                trainingLevel,
                totalHours,
                1,
                active == null || active,
                0,
                now,
                requireNonNull(actorId, "actorId"),
                now,
                actorId,
                null,
                null);
    }

    public static TrainingProgram reconstitute(UUID id,
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
        return new TrainingProgram(
                id,
                knowledgeNetworkId,
                programCode,
                name,
                trainingLevel,
                totalHours,
                version,
                active,
                rowVersion,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy,
                deletedAt,
                deletedBy);
    }

    public void applyUpdate(UUID knowledgeNetworkId,
                            String programCode,
                            String name,
                            String trainingLevel,
                            Integer totalHours,
                            Integer version,
                            Boolean isActive,
                            UUID actorId) {
        if (knowledgeNetworkId != null) {
            this.knowledgeNetworkId = knowledgeNetworkId;
        }
        if (programCode != null) {
            this.programCode = requireProgramCode(programCode);
        }
        if (name != null) {
            this.name = requireName(name);
        }
        if (trainingLevel != null) {
            this.trainingLevel = requireTrainingLevel(trainingLevel);
        }
        if (totalHours != null) {
            this.totalHours = requireTotalHours(totalHours);
        }
        if (version != null) {
            this.version = version;
        }
        if (isActive != null) {
            this.active = isActive;
        }
        this.updatedBy = requireNonNull(actorId, "actorId");
        this.updatedAt = Instant.now();
    }

    /**
     * Full soft delete required by HU-ACAD-05.
     * The row remains in the database so existing ficha foreign keys remain valid.
     */
    public void deactivate(UUID actorId) {
        UUID validatedActor = requireNonNull(actorId, "actorId");
        Instant now = Instant.now();
        this.active = false;
        this.deletedAt = now;
        this.deletedBy = validatedActor;
        this.updatedAt = now;
        this.updatedBy = validatedActor;
    }

    private static String requireProgramCode(String programCode) {
        if (programCode == null || programCode.isBlank()) {
            throw new IllegalArgumentException("programCode is required");
        }
        if (programCode.length() > 20) {
            throw new IllegalArgumentException("programCode must not exceed 20 characters");
        }
        return programCode;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("name must not exceed 200 characters");
        }
        return name;
    }

    private static String requireTrainingLevel(String trainingLevel) {
        if (trainingLevel == null || !ALLOWED_TRAINING_LEVELS.contains(trainingLevel)) {
            throw new IllegalArgumentException(
                    "trainingLevel must be one of AUXILIARY, OPERATOR, TECHNICIAN, TECHNOLOGIST");
        }
        return trainingLevel;
    }

    private static int requireTotalHours(int totalHours) {
        if (totalHours < 0) {
            throw new IllegalArgumentException("totalHours must be >= 0");
        }
        return totalHours;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
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
