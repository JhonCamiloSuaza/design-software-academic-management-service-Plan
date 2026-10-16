package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.Competency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueryCompetenciesUseCase {
    Competency getById(UUID id);

    Page list(Query query);

    record Query(UUID programId,
                 Optional<Boolean> isActive,
                 int page,
                 int size,
                 Optional<String> sort) {
    }

    record Page(List<Competency> content,
                long totalElements,
                int page,
                int size) {
    }
}
