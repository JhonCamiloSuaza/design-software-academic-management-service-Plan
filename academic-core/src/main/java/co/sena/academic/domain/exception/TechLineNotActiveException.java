package co.sena.academic.domain.exception;

import java.util.UUID;

/**
 * Thrown when a tech_network is created/updated referencing a tech_line
 * that exists but is NOT active. This is an application rule — the DB
 * FK (fk_tech_network_tech_line) does not distinguish is_active
 * (per HU-ACAD-02's "Detalle técnico").
 *
 * NOTE (needs team confirmation, same as ACAD_TECH_LINE_CODE_ALREADY_EXISTS
 * in HU-ACAD-01's DoR): mapped here to 409 CONFLICT with
 * ACAD_TECH_LINE_NOT_ACTIVE — the Gherkin only specifies the status for
 * the "does not exist" case (404), not for "exists but inactive".
 */
public class TechLineNotActiveException extends RuntimeException {

    public TechLineNotActiveException(UUID techLineId) {
        super("tech_line with id=" + techLineId + " exists but is not active");
    }
}