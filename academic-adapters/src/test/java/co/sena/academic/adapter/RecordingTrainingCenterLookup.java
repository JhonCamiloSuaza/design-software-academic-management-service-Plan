package co.sena.academic.adapter;

import co.sena.academic.application.port.out.TrainingCenterLookupPort;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Explicit mock of reference-data-service for HU-ACAD-08 integration tests.
 * It keeps the external dependency visible instead of silently accepting IDs
 * in production.
 */
public class RecordingTrainingCenterLookup implements TrainingCenterLookupPort {

    private final Map<UUID, Boolean> activeById = new ConcurrentHashMap<>();
    private final Set<UUID> missingIds = ConcurrentHashMap.newKeySet();

    @Override
    public Optional<TrainingCenterReference> findById(UUID trainingCenterId) {
        if (missingIds.contains(trainingCenterId)) {
            return Optional.empty();
        }
        return Optional.of(new TrainingCenterReference(
                activeById.getOrDefault(trainingCenterId, true)));
    }

    public void markInactive(UUID trainingCenterId) {
        activeById.put(trainingCenterId, false);
        missingIds.remove(trainingCenterId);
    }

    public void markMissing(UUID trainingCenterId) {
        missingIds.add(trainingCenterId);
        activeById.remove(trainingCenterId);
    }

    public void clear() {
        activeById.clear();
        missingIds.clear();
    }
}
