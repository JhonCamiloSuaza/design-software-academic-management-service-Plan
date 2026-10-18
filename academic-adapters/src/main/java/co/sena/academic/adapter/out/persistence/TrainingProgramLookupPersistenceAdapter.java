package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.out.TrainingProgramLookupPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TrainingProgramLookupPersistenceAdapter implements TrainingProgramLookupPort {

    private final TrainingProgramRefJpaRepository repository;

    public TrainingProgramLookupPersistenceAdapter(TrainingProgramRefJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsActiveByKnowledgeNetworkId(UUID knowledgeNetworkId) {
        return repository.existsByKnowledgeNetworkIdAndActive(knowledgeNetworkId, true);
    }
}