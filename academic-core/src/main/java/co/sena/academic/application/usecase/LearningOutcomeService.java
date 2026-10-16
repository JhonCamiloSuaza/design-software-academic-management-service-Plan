package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.*;
import co.sena.academic.application.port.out.CompetencyRepositoryPort;
import co.sena.academic.application.port.out.LearningOutcomeRepositoryPort;
import co.sena.academic.domain.exception.*;
import co.sena.academic.domain.model.Competency;
import co.sena.academic.domain.model.LearningOutcome;
import java.util.UUID;

/** HU-ACAD-07 application service. */
public class LearningOutcomeService implements CreateLearningOutcomeUseCase, UpdateLearningOutcomeUseCase, DeactivateLearningOutcomeUseCase, QueryLearningOutcomesUseCase {
    private final LearningOutcomeRepositoryPort repository; private final CompetencyRepositoryPort competencyRepository;
    public LearningOutcomeService(LearningOutcomeRepositoryPort repository, CompetencyRepositoryPort competencyRepository){this.repository=repository;this.competencyRepository=competencyRepository;}
    @Override public LearningOutcome create(CreateLearningOutcomeUseCase.Command command){ ensureCompetencyActive(command.competencyId()); if(repository.existsByCompetencyIdAndCode(command.competencyId(),command.code()))throw new LearningOutcomeCodeAlreadyExistsException(command.competencyId(),command.code()); return repository.save(LearningOutcome.create(command.competencyId(),command.code(),command.description(),command.isActive(),command.actorId())); }
    @Override public LearningOutcome update(UpdateLearningOutcomeUseCase.Command command){ LearningOutcome outcome=required(command.id()); mutable(outcome); version(outcome,command.rowVersion()); String code=command.code()==null?outcome.getCode():command.code(); if(repository.existsByCompetencyIdAndCodeAndIdNot(outcome.getCompetencyId(),code,outcome.getId()))throw new LearningOutcomeCodeAlreadyExistsException(outcome.getCompetencyId(),code); outcome.update(command.code(),command.description(),command.isActive(),command.actorId()); return repository.save(outcome); }
    @Override public void deactivate(DeactivateLearningOutcomeUseCase.Command command){ LearningOutcome outcome=required(command.id()); outcome.deactivate(command.actorId()); repository.save(outcome); }
    @Override public Page list(Query query){ competency(query.competencyId()); var content=repository.findAll(query.competencyId(),query.isActive(),query.page(),query.size(),query.sort()); return new Page(content,repository.count(query.competencyId(),query.isActive()),query.page(),query.size()); }
    private LearningOutcome required(UUID id){return repository.findById(id).orElseThrow(()->new LearningOutcomeNotFoundException(id));}
    private Competency competency(UUID id){return competencyRepository.findById(id).orElseThrow(()->new CompetencyNotFoundException(id));}
    private void ensureCompetencyActive(UUID id){Competency c=competency(id);if(!c.isActive()||c.getDeletedAt()!=null)throw new CompetencyDeactivatedException(id);}
    private void mutable(LearningOutcome l){if(!l.isActive()||l.getDeletedAt()!=null)throw new IllegalStateException("Learning outcome is deactivated");}
    private void version(LearningOutcome l,int version){if(l.getRowVersion()!=version)throw new RowVersionMismatchException(l.getId(),version,l.getRowVersion());}
}
