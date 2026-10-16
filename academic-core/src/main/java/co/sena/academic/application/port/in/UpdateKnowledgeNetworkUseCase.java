package co.sena.academic.application.port.in;

import co.sena.academic.domain.model.KnowledgeNetwork;

import java.util.UUID;

/**
 * PUT /knowledge-networks/{id}. Requires ACADEMIC_PROGRAM_MANAGE.
 * Full replace of tech_network_id/name/code/is_active. 404 if it does not exist,
 * 409 if the new code clashes with another knowledge_network under the
 * same tech_network_id.
 *
 * Not spelled out as its own Gherkin scenario in the HU text (only E1/E2/E3
 * are enumerated), but the INVEST description explicitly asks for
 * "crear, consultar, actualizar y dar de baja" — included here for
 * completeness/symmetry with HU-ACAD-01 (tech_line). Confirm with the PO
 * if this should be split into its own follow-up HU instead.
 */
public interface UpdateKnowledgeNetworkUseCase {

    KnowledgeNetwork update(Command command);

    record Command(UUID id, UUID techNetworkId, String name, String code, Boolean isActive, UUID actorId) {
    }
}