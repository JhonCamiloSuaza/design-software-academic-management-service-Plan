package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.Ficha;

import java.time.LocalDate;
import java.util.UUID;

public interface CreateFichaUseCase {

    Ficha create(Command command);

    record Command(
            String fichaNumber,
            UUID programId,
            UUID trainingCenterId,
            LocalDate startDate,
            LocalDate expectedEndDate,
            String trainingShift,
            String trainingModality,
            int maxCapacity,
            UUID actorId) {
    }
}
