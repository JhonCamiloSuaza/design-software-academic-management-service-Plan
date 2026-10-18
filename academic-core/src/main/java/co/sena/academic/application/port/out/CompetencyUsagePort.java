package co.sena.academic.application.port.out;

import java.util.UUID;

/**
 * HU-ACAD-06 E3 guard: determines whether a competency belongs to curriculum
 * currently used by a non-terminal ficha of the same training program.
 */
public interface CompetencyUsagePort {
    boolean isUsedByOpenFichas(UUID competencyId, UUID programId);
}
