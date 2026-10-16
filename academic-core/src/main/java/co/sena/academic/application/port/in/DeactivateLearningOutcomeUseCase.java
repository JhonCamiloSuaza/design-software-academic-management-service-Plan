package co.sena.academic.application.port.in; import java.util.UUID; public interface DeactivateLearningOutcomeUseCase { void deactivate(Command command); record Command(UUID id,UUID actorId){} }
