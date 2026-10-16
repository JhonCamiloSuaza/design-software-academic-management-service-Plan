package co.sena.academic.application.port.out;
import co.sena.academic.domain.model.LearningOutcome; import java.util.*;
public interface LearningOutcomeRepositoryPort { LearningOutcome save(LearningOutcome value); Optional<LearningOutcome> findById(UUID id); boolean existsByCompetencyIdAndCode(UUID competencyId,String code); boolean existsByCompetencyIdAndCodeAndIdNot(UUID competencyId,String code,UUID id); List<LearningOutcome> findAll(UUID competencyId,Optional<Boolean> active,int page,int size,Optional<String> sort); long count(UUID competencyId,Optional<Boolean> active); }
