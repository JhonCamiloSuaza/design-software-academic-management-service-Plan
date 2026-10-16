package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/** Strict HU-ACAD-10 request: unknown and immutable properties are rejected. */
@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateFichaRequest {

    @Pattern(regexp = "DAY|NIGHT|MIXED", message = "training_shift must be DAY, NIGHT or MIXED")
    private String trainingShift;
    private boolean trainingShiftPresent;

    @Pattern(regexp = "IN_PERSON|VIRTUAL|HYBRID",
            message = "training_modality must be IN_PERSON, VIRTUAL or HYBRID")
    private String trainingModality;
    private boolean trainingModalityPresent;

    private LocalDate expectedEndDate;
    private boolean expectedEndDatePresent;

    @Min(value = 1, message = "max_capacity must be >= 1")
    @Max(value = Integer.MAX_VALUE, message = "max_capacity is out of range")
    private Integer maxCapacity;
    private boolean maxCapacityPresent;

    @NotNull(message = "row_version is required")
    @PositiveOrZero(message = "row_version must be >= 0")
    private Integer rowVersion;

    @JsonSetter("training_shift")
    public void setTrainingShift(String value) {
        trainingShiftPresent = true;
        trainingShift = value;
    }

    @JsonSetter("training_modality")
    public void setTrainingModality(String value) {
        trainingModalityPresent = true;
        trainingModality = value;
    }

    @JsonSetter("expected_end_date")
    public void setExpectedEndDate(LocalDate value) {
        expectedEndDatePresent = true;
        expectedEndDate = value;
    }

    @JsonSetter("max_capacity")
    public void setMaxCapacity(Integer value) {
        maxCapacityPresent = true;
        maxCapacity = value;
    }

    @JsonSetter("row_version")
    public void setRowVersion(Integer value) {
        rowVersion = value;
    }

    @AssertTrue(message = "training_shift cannot be null when provided")
    @JsonIgnore
    public boolean isTrainingShiftValueValid() {
        return !trainingShiftPresent || trainingShift != null;
    }

    @AssertTrue(message = "training_modality cannot be null when provided")
    @JsonIgnore
    public boolean isTrainingModalityValueValid() {
        return !trainingModalityPresent || trainingModality != null;
    }

    @AssertTrue(message = "max_capacity cannot be null when provided")
    @JsonIgnore
    public boolean isMaxCapacityValueValid() {
        return !maxCapacityPresent || maxCapacity != null;
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException(
                "Property '" + property + "' is not editable by HU-ACAD-10");
    }

    public String trainingShift() { return trainingShift; }
    public boolean trainingShiftPresent() { return trainingShiftPresent; }
    public String trainingModality() { return trainingModality; }
    public boolean trainingModalityPresent() { return trainingModalityPresent; }
    public LocalDate expectedEndDate() { return expectedEndDate; }
    public boolean expectedEndDatePresent() { return expectedEndDatePresent; }
    public Integer maxCapacity() { return maxCapacity; }
    public boolean maxCapacityPresent() { return maxCapacityPresent; }
    public Integer rowVersion() { return rowVersion; }
}
