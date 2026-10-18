package co.sena.academic.application.port.out;

import java.util.UUID;

/**
 * Narrow read-only output port: what TechNetworkService needs to know
 * about tech_line, WITHOUT depending on the full TechLineRepositoryPort
 * (which belongs to HU-ACAD-01's own aggregate). Same convention as
 * TechNetworkLookupPort used by KnowledgeNetworkService in HU-ACAD-03.
 */
public interface TechLineLookupPort {

    boolean existsById(UUID id);

    /** Only meaningful when existsById(id) is true. */
    boolean isActiveById(UUID id);
}