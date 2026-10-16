package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.Ficha;

import java.time.LocalDate;
import java.util.UUID;

public interface ChangeFichaStatusUseCase {

    Ficha changeStatus(Command command);

    record Command(
            UUID id,
            String status,
            LocalDate actualEndDate,
            QueryFichasUseCase.AccessContext access,
            boolean centerDirector) {
    }
}
