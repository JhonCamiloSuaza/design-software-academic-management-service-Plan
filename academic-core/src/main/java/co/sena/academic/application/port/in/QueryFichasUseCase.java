package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.Ficha;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read use case for HU-ACAD-09. */
public interface QueryFichasUseCase {

    Ficha getById(UUID id, AccessContext access);

    Page list(Query query);

    enum ScopeType {
        GLOBAL,
        TRAINING_CENTER,
        OWN_FICHAS,
        OWN_FICHA_AS_LEARNER
    }

    record AccessContext(
            UUID userId,
            UUID actorId,
            UUID trainingCenterId,
            ScopeType scope) {
    }

    record Query(
            Optional<UUID> programId,
            Optional<String> status,
            Optional<String> fichaNumber,
            Optional<UUID> trainingCenterId,
            Optional<LocalDate> from,
            Optional<LocalDate> to,
            int page,
            int size,
            Optional<String> sort,
            AccessContext access) {

        public Query(UUID programId,
                     String status,
                     String fichaNumber,
                     UUID trainingCenterId,
                     LocalDate from,
                     LocalDate to,
                     int page,
                     int size,
                     String sort,
                     AccessContext access) {
            this(Optional.ofNullable(programId),
                    Optional.ofNullable(status),
                    Optional.ofNullable(fichaNumber),
                    Optional.ofNullable(trainingCenterId),
                    Optional.ofNullable(from),
                    Optional.ofNullable(to),
                    page,
                    size,
                    Optional.ofNullable(sort),
                    access);
        }
    }

    record Page(List<Ficha> content, long totalElements, int page, int size) {
    }
}
