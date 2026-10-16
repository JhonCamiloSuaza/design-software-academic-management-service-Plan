package co.sena.academic.domain.exception;

import java.util.UUID;

/**
 * Thrown when looking up/updating/deactivating a technical network whose
 * id does not exist. The REST adapter translates this to 404.
 *
 * Also thrown by CreateKnowledgeNetworkUseCase (HU-ACAD-03, E1) when
 * tech_network_id does not reference an existing row in
 * academic_management.tech_network — "tech_network" is not owned by that
 * HU (it belongs to this sibling HU, second level of the hierarchy), it
 * only needs to know whether it exists, via TechNetworkLookupPort.
 *
 * For HU-ACAD-03's case specifically, the REST adapter translates this to
 * 404 NOT FOUND with error code ACAD_TECH_NETWORK_NOT_FOUND, since the
 * client referenced a parent resource that does not exist. If Contracts
 * prefers 400/422 for an invalid reference inside the body instead of
 * 404, adjust KnowledgeNetworkExceptionHandler accordingly — confirm
 * with the Contracts team, per that HU's DoR.
 */
public class TechNetworkNotFoundException extends RuntimeException {

    public TechNetworkNotFoundException(UUID id) {
        super("No tech_network found with id=" + id);
    }
}