package co.sena.academic.domain.exception;

public class TrainingProgramCodeAlreadyExistsException extends RuntimeException {
    public TrainingProgramCodeAlreadyExistsException(String programCode) {
        super("Training program code already exists: " + programCode);
    }
}
