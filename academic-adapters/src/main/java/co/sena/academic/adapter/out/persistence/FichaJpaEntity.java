package co.sena.academic.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "academic_management", name = "enrollment_ficha")
public class FichaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "ficha_number", nullable = false, length = 20)
    private String fichaNumber;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "program_version", nullable = false)
    private int programVersion;

    @Column(name = "training_center_id", nullable = false)
    private UUID trainingCenterId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expected_end_date")
    private LocalDate expectedEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "training_shift", nullable = false, length = 20)
    private String trainingShift;

    @Column(name = "training_modality", nullable = false, length = 20)
    private String trainingModality;

    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity;

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

    protected FichaJpaEntity() {
    }

    FichaJpaEntity(UUID id,
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
        this.id = id;
        this.fichaNumber = fichaNumber;
        this.programId = programId;
        this.programVersion = programVersion;
        this.trainingCenterId = trainingCenterId;
        this.status = status;
        this.startDate = startDate;
        this.expectedEndDate = expectedEndDate;
        this.actualEndDate = actualEndDate;
        this.trainingShift = trainingShift;
        this.trainingModality = trainingModality;
        this.maxCapacity = maxCapacity;
        this.rowVersion = rowVersion;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
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
