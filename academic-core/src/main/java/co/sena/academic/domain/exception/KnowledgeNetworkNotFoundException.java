package co.sena.academic.domain.exception;

import java.util.UUID;

/**
 * Thrown when looking up/updating/deactivating a knowledge_network whose
 * id does not exist. The REST adapter translates this to 404.
 */
public class KnowledgeNetworkNotFoundException extends RuntimeException {

    public KnowledgeNetworkNotFoundException(UUID id) {
        super("No knowledge_network found with id=" + id);
    }
}