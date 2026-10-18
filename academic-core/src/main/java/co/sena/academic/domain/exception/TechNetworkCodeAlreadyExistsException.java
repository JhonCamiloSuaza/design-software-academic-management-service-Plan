package co.sena.academic.domain.exception;

import java.util.UUID;

/**
 * Thrown when "code" already exists for the same tech_line_id in
 * academic_management.tech_network (uq_tech_network_tech_line_id_code —
 * unique PER LINE, not global). The REST adapter translates this to
 * 409 CONFLICT with error code ACAD_TECH_NETWORK_CODE_ALREADY_EXISTS.
 */
public class TechNetworkCodeAlreadyExistsException extends RuntimeException {

    public TechNetworkCodeAlreadyExistsException(UUID techLineId, String code) {
        super("A technical network with code=" + code + " already exists under tech_line_id=" + techLineId);
    }
}