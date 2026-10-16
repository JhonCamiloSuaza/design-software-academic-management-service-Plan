package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.KnowledgeNetwork;

import java.util.UUID;

/**
 * E1 — POST /knowledge-networks. Requires ACADEMIC_PROGRAM_MANAGE in the
 * REST adapter. Validates that tech_network_id exists (else
 * TechNetworkNotFoundException) and that code is unique per
 * tech_network_id (else KnowledgeNetworkCodeAlreadyExistsException).
 */
public interface CreateKnowledgeNetworkUseCase {

    KnowledgeNetwork create(Command command);

    record Command(UUID techNetworkId, String name, String code, UUID actorId) {
    }
}