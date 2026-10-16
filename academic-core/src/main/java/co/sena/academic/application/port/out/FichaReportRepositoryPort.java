package co.sena.academic.application.port.out;

import co.sena.academic.application.port.in.QueryFichaReportsUseCase;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only projections owned by academic-management reporting. */
public interface FichaReportRepositoryPort {

    List<QueryFichaReportsUseCase.EnrollmentByFichaItem> findEnrollmentByFicha(
            Filter filter, int page, int size, Optional<String> sort);

    long countEnrollmentByFicha(Filter filter);

    List<ProgressSource> findFichaProgress(Filter filter, int page, int size, Optional<String> sort);

    long countFichaProgress(Filter filter);

    record Filter(Optional<UUID> trainingCenterId,
                  Optional<UUID> programId,
                  Optional<String> status,
                  Optional<LocalDate> from,
                  Optional<LocalDate> to) {
    }

    record ProgressSource(UUID fichaId,
                          String fichaNumber,
                          String status,
                          LocalDate startDate,
                          LocalDate expectedEndDate,
                          LocalDate actualEndDate) {
    }
}
