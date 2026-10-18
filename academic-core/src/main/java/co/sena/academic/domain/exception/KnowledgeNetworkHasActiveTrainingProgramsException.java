package co.sena.academic.domain.exception;

import java.util.UUID;

/**
 * E3: thrown when trying to deactivate ("dar de baja") a knowledge_network
 * that still has training_program rows with is_active=true pointing at it.
 *
 * This is an APPLICATION rule, not a database one: fk_knowledge_network_tech_network
 * and any FK from training_program to knowledge_network only protect
 * PHYSICAL deletes (DELETE FROM ...), but DELETE /knowledge-networks/{id}
 * only performs a LOGICAL deactivation (is_active=false, no deleted_at
 * column exists), so the real RESTRICT constraint never fires here —
 * the use case has to check dependents itself (TrainingProgramLookupPort).
 *
 * The REST adapter translates this to 409 CONFLICT with error code
 * ACAD_KNOWLEDGE_NETWORK_HAS_ACTIVE_TRAINING_PROGRAMS.
 */
public class KnowledgeNetworkHasActiveTrainingProgramsException extends RuntimeException {

    public KnowledgeNetworkHasActiveTrainingProgramsException(UUID knowledgeNetworkId) {
        super("knowledge_network id=" + knowledgeNetworkId
                + " has active training_program rows and cannot be deactivated");
    }
}