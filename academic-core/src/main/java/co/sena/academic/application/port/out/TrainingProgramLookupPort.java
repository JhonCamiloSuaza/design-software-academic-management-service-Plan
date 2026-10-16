package co.sena.academic.application.port.out;

import java.util.UUID;

/**
 * Output port used ONLY to check, on deactivation (E3), whether a
 * knowledge_network still has training_program dependents with
 * is_active=true. This service does not own the training_program
 * aggregate (it hangs from knowledge_network, but is a separate HU) —
 * this is a narrow, read-only lookup, not a full repository.
 */
public interface TrainingProgramLookupPort {

    boolean existsActiveByKnowledgeNetworkId(UUID knowledgeNetworkId);
}