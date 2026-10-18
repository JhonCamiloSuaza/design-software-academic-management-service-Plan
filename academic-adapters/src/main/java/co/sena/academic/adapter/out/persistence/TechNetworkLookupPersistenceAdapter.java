package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.out.TechNetworkLookupPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TechNetworkLookupPersistenceAdapter implements TechNetworkLookupPort {

    private final TechNetworkJpaRepository repository;

    public TechNetworkLookupPersistenceAdapter(TechNetworkJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsById(UUID techNetworkId) {
        return repository.existsById(techNetworkId);
    }
}