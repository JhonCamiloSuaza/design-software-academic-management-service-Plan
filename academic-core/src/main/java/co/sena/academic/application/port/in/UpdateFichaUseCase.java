package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.Ficha;

import java.time.LocalDate;
import java.util.UUID;

/** HU-ACAD-10: update only the operational data of an existing ficha. */
public interface UpdateFichaUseCase {

    Ficha update(Command command);

    record Command(
            UUID id,
            String trainingShift,
            boolean trainingShiftPresent,
            String trainingModality,
            boolean trainingModalityPresent,
            LocalDate expectedEndDate,
            boolean expectedEndDatePresent,
            Integer maxCapacity,
            boolean maxCapacityPresent,
            int rowVersion,
            QueryFichasUseCase.AccessContext access) {
    }
}
