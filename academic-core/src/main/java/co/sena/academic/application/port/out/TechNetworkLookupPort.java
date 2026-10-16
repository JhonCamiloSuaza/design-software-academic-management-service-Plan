package co.sena.academic.application.port.out;

import java.util.UUID;

/**
 * Output port used ONLY to validate that a tech_network_id given on
 * creation (E1) actually exists in academic_management.tech_network.
 * This service does not own the tech_network aggregate (it belongs to
 * the sibling HU for the second level of the hierarchy) — this is a
 * narrow, read-only lookup, not a full repository.
 */
public interface TechNetworkLookupPort {

    boolean existsById(UUID techNetworkId);
}