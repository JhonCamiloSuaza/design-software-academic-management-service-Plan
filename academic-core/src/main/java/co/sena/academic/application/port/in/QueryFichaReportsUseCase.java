package co.sena.academic.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only reporting use cases for HU-ACAD-14 and HU-ACAD-15. */
public interface QueryFichaReportsUseCase {

    EnrollmentByFichaPage enrollmentByFicha(Query query);

    FichaProgressPage fichaProgress(Query query);

    record Query(
            Optional<UUID> trainingCenterId,
            Optional<UUID> programId,
            Optional<String> status,
            Optional<LocalDate> from,
            Optional<LocalDate> to,
            int page,
            int size,
            Optional<String> sort,
            QueryFichasUseCase.AccessContext access) {

        public Query(UUID trainingCenterId,
                     UUID programId,
                     String status,
                     LocalDate from,
                     LocalDate to,
                     int page,
                     int size,
                     String sort,
                     QueryFichasUseCase.AccessContext access) {
            this(Optional.ofNullable(trainingCenterId),
                    Optional.ofNullable(programId),
                    Optional.ofNullable(status),
                    Optional.ofNullable(from),
                    Optional.ofNullable(to),
                    page,
                    size,
                    Optional.ofNullable(sort),
                    access);
        }
    }

    record EnrollmentByFichaPage(List<EnrollmentByFichaItem> content,
                                 long totalElements,
                                 int page,
                                 int size) {
    }

    record EnrollmentByFichaItem(UUID fichaId,
                                 String fichaNumber,
                                 UUID programId,
                                 String programCode,
                                 String programName,
                                 UUID trainingCenterId,
                                 String status,
                                 int maxCapacity,
                                 String trainingShift,
                                 String trainingModality,
                                 LocalDate startDate) {
    }

    record FichaProgressPage(List<FichaProgressItem> content,
                             long totalElements,
                             int page,
                             int size) {
    }

    record FichaProgressItem(UUID fichaId,
                             String fichaNumber,
                             String status,
                             LocalDate startDate,
                             LocalDate expectedEndDate,
                             LocalDate actualEndDate,
                             int daysElapsed,
                             Integer daysRemaining,
                             boolean delayed) {
    }
}
