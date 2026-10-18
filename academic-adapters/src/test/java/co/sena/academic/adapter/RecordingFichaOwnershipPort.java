package co.sena.academic.adapter;

import co.sena.academic.application.port.out.FichaOwnershipPort;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RecordingFichaOwnershipPort implements FichaOwnershipPort {

    private final Map<UUID, Set<UUID>> instructorFichas = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> learnerFichas = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> activeLearners = new ConcurrentHashMap<>();

    @Override
    public Set<UUID> findFichaIdsForInstructor(UUID instructorId) {
        return instructorFichas.getOrDefault(instructorId, Set.of());
    }

    @Override
    public Set<UUID> findFichaIdsForLearner(UUID learnerId) {
        return learnerFichas.getOrDefault(learnerId, Set.of());
    }

    @Override
    public int countActiveLearners(UUID fichaId) {
        return activeLearners.getOrDefault(fichaId, 0);
    }

    public void instructorOwns(UUID actorId, UUID... fichaIds) {
        instructorFichas.put(actorId, Set.of(fichaIds));
    }

    public void learnerOwns(UUID actorId, UUID... fichaIds) {
        learnerFichas.put(actorId, Set.of(fichaIds));
    }

    public void activeLearners(UUID fichaId, int count) {
        activeLearners.put(fichaId, count);
    }

    public void clear() {
        instructorFichas.clear();
        learnerFichas.clear();
        activeLearners.clear();
    }
}
