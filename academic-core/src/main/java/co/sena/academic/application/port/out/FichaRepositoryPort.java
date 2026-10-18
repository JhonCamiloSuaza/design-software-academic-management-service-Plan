package co.sena.academic.application.port.out;

import co.sena.academic.domain.model.Ficha;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FichaRepositoryPort {

    Ficha save(Ficha ficha);

    Optional<Ficha> findById(UUID id);

    boolean existsByFichaNumber(String fichaNumber);

    List<Ficha> findAll(
            Optional<UUID> programId,
            Optional<String> status,
            Optional<String> fichaNumber,
            Optional<UUID> trainingCenterId,
            Optional<LocalDate> from,
            Optional<LocalDate> to,
            Optional<Set<UUID>> allowedFichaIds,
            int page,
            int size,
            Optional<String> sort);

    long count(
            Optional<UUID> programId,
            Optional<String> status,
            Optional<String> fichaNumber,
            Optional<UUID> trainingCenterId,
            Optional<LocalDate> from,
            Optional<LocalDate> to,
            Optional<Set<UUID>> allowedFichaIds);
}
