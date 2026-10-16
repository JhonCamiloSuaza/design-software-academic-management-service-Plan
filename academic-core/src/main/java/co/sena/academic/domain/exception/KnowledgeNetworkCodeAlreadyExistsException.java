package co.sena.academic.domain.exception;

import java.util.UUID;

/**
 * Thrown when "code" already exists for the same tech_network_id in
 * academic_management.knowledge_network (uq_knowledge_network_tech_network_id_code
 * constraint — the uniqueness is scoped PER technical network, unlike
 * tech_line's uq_tech_line_code which is global). The REST adapter
 * translates this to 409 CONFLICT with error code
 * ACAD_KNOWLEDGE_NETWORK_CODE_ALREADY_EXISTS (additive extension
 * proposed to error-codes.yaml — see the HU's DoR).
 */
public class KnowledgeNetworkCodeAlreadyExistsException extends RuntimeException {

    private final UUID techNetworkId;
    private final String code;

    public KnowledgeNetworkCodeAlreadyExistsException(UUID techNetworkId, String code) {
        super("A knowledge_network with code=" + code + " already exists for tech_network_id=" + techNetworkId);
        this.techNetworkId = techNetworkId;
        this.code = code;
    }

    public UUID getTechNetworkId() {
        return techNetworkId;
    }

    public String getCode() {
        return code;
    }
}