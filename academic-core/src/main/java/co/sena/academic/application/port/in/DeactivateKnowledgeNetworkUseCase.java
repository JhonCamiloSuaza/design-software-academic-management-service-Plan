package co.sena.academic.application.port.in;

import java.util.UUID;

/**
 * E3 — DELETE /knowledge-networks/{id}. Sets is_active=false (there is no
 * deleted_at in the real DDL — same convention as tech_line's E4).
 * Requires ACADEMIC_PROGRAM_MANAGE.
 *
 * Responds 409 (KnowledgeNetworkHasActiveTrainingProgramsException) if
 * the knowledge_network has training_program rows with is_active=true —
 * this is an application-level check (TrainingProgramLookupPort), because
 * the real FK RESTRICT only protects physical deletes, not this logical
 * deactivation.
 */
public interface DeactivateKnowledgeNetworkUseCase {

    void deactivate(UUID id, UUID actorId);
}