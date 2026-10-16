package co.sena.academic.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import co.sena.academic.domain.exception.FichaCapacityIncreaseNotAllowedException;
import co.sena.academic.domain.exception.FichaInvalidStatusTransitionException;
import co.sena.academic.domain.exception.FichaTerminalStatusException;

/** Minimal ficha aggregate needed by the HU-ACAD-05 opening invariant. */
public final class Ficha {

    private static final Set<String> SHIFTS = Set.of("DAY", "NIGHT", "MIXED");
    private static final Set<String> MODALITIES = Set.of("IN_PERSON", "VIRTUAL", "HYBRID");

    private final UUID id;
    private final String fichaNumber;
    private final UUID programId;
    private final int programVersion;
    private final UUID trainingCenterId;
    private final String status;
    private final LocalDate startDate;
    private final LocalDate expectedEndDate;
    private final LocalDate actualEndDate;
    private final String trainingShift;
    private final String trainingModality;
    private final int maxCapacity;
    private final int rowVersion;
    private final Instant createdAt;
    private final UUID createdBy;
    private final Instant updatedAt;
    private final UUID updatedBy;

    private Ficha(UUID id,
                  String fichaNumber,
                  UUID programId,
                  int programVersion,
                  UUID trainingCenterId,
                  String status,
                  LocalDate startDate,
                  LocalDate expectedEndDate,
                  LocalDate actualEndDate,
                  String trainingShift,
                  String trainingModality,
                  int maxCapacity,
                  int rowVersion,
                  Instant createdAt,
                  UUID createdBy,
                  Instant updatedAt,
                  UUID updatedBy) {
        this.id = requireNonNull(id, "id");
        this.fichaNumber = requireFichaNumber(fichaNumber);
        this.programId = requireNonNull(programId, "programId");
        if (programVersion < 1) {
            throw new IllegalArgumentException("programVersion must be >= 1");
        }
        this.programVersion = programVersion;
        this.trainingCenterId = requireNonNull(trainingCenterId, "trainingCenterId");
        this.status = requireNonNull(status, "status");
        this.startDate = requireNonNull(startDate, "startDate");
        this.expectedEndDate = expectedEndDate;
        this.actualEndDate = actualEndDate;
        this.trainingShift = requireShift(trainingShift);
        this.trainingModality = requireModality(trainingModality);
        if (maxCapacity < 1) {
            throw new IllegalArgumentException("maxCapacity must be >= 1");
        }
        this.maxCapacity = maxCapacity;
        this.rowVersion = rowVersion;
        this.createdAt = requireNonNull(createdAt, "createdAt");
        this.createdBy = requireNonNull(createdBy, "createdBy");
        this.updatedAt = requireNonNull(updatedAt, "updatedAt");
        this.updatedBy = requireNonNull(updatedBy, "updatedBy");
    }

    public static Ficha create(String fichaNumber,
                               UUID programId,
                               int programVersion,
                               UUID trainingCenterId,
                               LocalDate startDate,
                               LocalDate expectedEndDate,
                               String trainingShift,
                               String trainingModality,
                               int maxCapacity,
                               UUID actorId) {
        Instant now = Instant.now();
        return new Ficha(
                UUID.randomUUID(),
                fichaNumber,
                programId,
                programVersion,
                trainingCenterId,
                "INDUCTION",
                startDate,
                expectedEndDate,
                null,
                trainingShift,
                trainingModality,
                maxCapacity,
                0,
                now,
                actorId,
                now,
                actorId);
    }

    public static Ficha reconstitute(UUID id,
                                     String fichaNumber,
                                     UUID programId,
                                     int programVersion,
                                     UUID trainingCenterId,
                                     String status,
                                     LocalDate startDate,
                                     LocalDate expectedEndDate,
                                     LocalDate actualEndDate,
                                     String trainingShift,
                                     String trainingModality,
                                     int maxCapacity,
                                     int rowVersion,
                                     Instant createdAt,
                                     UUID createdBy,
                                     Instant updatedAt,
                                     UUID updatedBy) {
        return new Ficha(id, fichaNumber, programId, programVersion, trainingCenterId, status,
                startDate, expectedEndDate, actualEndDate, trainingShift, trainingModality,
                maxCapacity, rowVersion, createdAt, createdBy, updatedAt, updatedBy);
    }

    /**
     * Applies the fields explicitly allowed by HU-ACAD-10.
     * Identity, lifecycle status, start/actual end dates and creation audit data stay unchanged.
     * The official Academic schema does not own learner_ficha_enrollment. The application
     * service validates the active-learner floor through Actors before calling this method.
     */
    public Ficha updateOperationalData(String trainingShift,
                                       String trainingModality,
                                       LocalDate expectedEndDate,
                                       int maxCapacity,
                                       UUID actorId) {
        ensureOperationalDataCanBeUpdated();
        if (maxCapacity > this.maxCapacity) {
            throw new FichaCapacityIncreaseNotAllowedException(id, this.maxCapacity, maxCapacity);
        }
        return new Ficha(
                id,
                fichaNumber,
                programId,
                programVersion,
                trainingCenterId,
                status,
                startDate,
                expectedEndDate,
                actualEndDate,
                trainingShift,
                trainingModality,
                maxCapacity,
                rowVersion,
                createdAt,
                createdBy,
                Instant.now(),
                requireNonNull(actorId, "actorId"));
    }

    public Ficha transitionStatus(String targetStatus, LocalDate requestedActualEndDate, UUID actorId) {
        ensureOperationalDataCanBeUpdated();
        requireValidTransition(status, targetStatus);
        Instant now = Instant.now();
        LocalDate effectiveActualEndDate = switch (targetStatus) {
            case "COMPLETED", "CANCELLED" -> requestedActualEndDate != null
                    ? requestedActualEndDate
                    : LocalDate.now();
            default -> null;
        };
        return new Ficha(
                id,
                fichaNumber,
                programId,
                programVersion,
                trainingCenterId,
                targetStatus,
                startDate,
                expectedEndDate,
                effectiveActualEndDate,
                trainingShift,
                trainingModality,
                maxCapacity,
                rowVersion,
                createdAt,
                createdBy,
                now,
                requireNonNull(actorId, "actorId"));
    }

    public void ensureOperationalDataCanBeUpdated() {
        if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
            throw new FichaTerminalStatusException(id, status);
        }
    }

    private void requireValidTransition(String currentStatus, String targetStatus) {
        boolean valid = switch (currentStatus) {
            case "INDUCTION" -> "EXECUTION".equals(targetStatus) || "CANCELLED".equals(targetStatus);
            case "EXECUTION" -> "PRODUCTIVE_STAGE".equals(targetStatus) || "CANCELLED".equals(targetStatus);
            case "PRODUCTIVE_STAGE" -> "COMPLETED".equals(targetStatus) || "CANCELLED".equals(targetStatus);
            default -> false;
        };
        if (!valid) {
            throw new FichaInvalidStatusTransitionException(id, currentStatus, targetStatus);
        }
    }

    private static String requireFichaNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("fichaNumber is required");
        }
        if (value.length() > 20) {
            throw new IllegalArgumentException("fichaNumber must not exceed 20 characters");
        }
        return value;
    }

    private static String requireShift(String value) {
        if (value == null || !SHIFTS.contains(value)) {
            throw new IllegalArgumentException("trainingShift must be one of DAY, NIGHT, MIXED");
        }
        return value;
    }

    private static String requireModality(String value) {
        if (value == null || !MODALITIES.contains(value)) {
            throw new IllegalArgumentException("trainingModality must be one of IN_PERSON, VIRTUAL, HYBRID");
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
    public String getFichaNumber() { return fichaNumber; }
    public UUID getProgramId() { return programId; }
    public int getProgramVersion() { return programVersion; }
    public UUID getTrainingCenterId() { return trainingCenterId; }
    public String getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getExpectedEndDate() { return expectedEndDate; }
    public LocalDate getActualEndDate() { return actualEndDate; }
    public String getTrainingShift() { return trainingShift; }
    public String getTrainingModality() { return trainingModality; }
    public int getMaxCapacity() { return maxCapacity; }
    public int getRowVersion() { return rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}
