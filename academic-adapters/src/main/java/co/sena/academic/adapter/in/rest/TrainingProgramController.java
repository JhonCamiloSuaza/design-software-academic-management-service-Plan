package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.rest.dto.CreateTrainingProgramRequest;
import co.sena.academic.adapter.in.rest.dto.DeactivateTrainingProgramRequest;
import co.sena.academic.adapter.in.rest.dto.TrainingProgramPageResponse;
import co.sena.academic.adapter.in.rest.dto.TrainingProgramResponse;
import co.sena.academic.adapter.in.rest.dto.UpdateTrainingProgramRequest;
import co.sena.academic.adapter.in.rest.mapper.TrainingProgramRestMapper;
import co.sena.academic.application.port.in.CreateTrainingProgramUseCase;
import co.sena.academic.application.port.in.DeactivateTrainingProgramUseCase;
import co.sena.academic.application.port.in.QueryTrainingProgramsUseCase;
import co.sena.academic.application.port.in.UpdateTrainingProgramUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** REST adapter for training program management. */
@RestController
@RequestMapping("/training-programs")
@Validated
public class TrainingProgramController {

    private final CreateTrainingProgramUseCase createUseCase;
    private final UpdateTrainingProgramUseCase updateUseCase;
    private final DeactivateTrainingProgramUseCase deactivateUseCase;
    private final QueryTrainingProgramsUseCase queryUseCase;
    private final TrainingProgramRestMapper mapper;

    public TrainingProgramController(CreateTrainingProgramUseCase createUseCase,
                                     UpdateTrainingProgramUseCase updateUseCase,
                                     DeactivateTrainingProgramUseCase deactivateUseCase,
                                     QueryTrainingProgramsUseCase queryUseCase,
                                     TrainingProgramRestMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deactivateUseCase = deactivateUseCase;
        this.queryUseCase = queryUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<TrainingProgramResponse> create(
            @Valid @RequestBody CreateTrainingProgramRequest request,
            @AuthenticationPrincipal UUID actorId) {
        var created = createUseCase.create(new CreateTrainingProgramUseCase.Command(
                request.knowledgeNetworkId(),
                request.programCode(),
                request.name(),
                request.trainingLevel(),
                request.totalHours(),
                request.isActive(),
                actorId));
        TrainingProgramResponse body = mapper.toResponse(created);
        return ResponseEntity.created(URI.create("/training-programs/" + body.id())).body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<TrainingProgramResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTrainingProgramRequest request,
            @AuthenticationPrincipal UUID actorId) {
        var updated = updateUseCase.update(new UpdateTrainingProgramUseCase.Command(
                id,
                request.knowledgeNetworkId(),
                request.programCode(),
                request.name(),
                request.trainingLevel(),
                request.totalHours(),
                request.version(),
                request.isActive(),
                request.rowVersion(),
                actorId));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @Valid @RequestBody DeactivateTrainingProgramRequest request,
            @AuthenticationPrincipal UUID actorId) {
        deactivateUseCase.deactivate(new DeactivateTrainingProgramUseCase.Command(
                id,
                request.rowVersion(),
                actorId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_VIEW')")
    public ResponseEntity<TrainingProgramResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(queryUseCase.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_VIEW')")
    public ResponseEntity<TrainingProgramPageResponse> list(
            @RequestParam(value = "knowledge_network_id", required = false) UUID knowledgeNetworkId,
            @RequestParam(value = "program_code", required = false) String programCode,
            @RequestParam(value = "training_level", required = false)
            @Pattern(
                    regexp = "AUXILIARY|OPERATOR|TECHNICIAN|TECHNOLOGIST",
                    message = "training_level must be one of AUXILIARY, OPERATOR, TECHNICIAN, TECHNOLOGIST")
            String trainingLevel,
            @RequestParam(value = "is_active", required = false) Boolean isActive,
            @RequestParam(name = "page", defaultValue = "1")
            @Min(value = 1, message = "page must be >= 1") int page,
            @RequestParam(name = "page_size", defaultValue = "20")
            @Min(value = 1, message = "page_size must be >= 1")
            @Max(value = 100, message = "page_size must be <= 100") int pageSize,
            @RequestParam(required = false) String sort) {
        var result = queryUseCase.list(new QueryTrainingProgramsUseCase.Query(
                knowledgeNetworkId,
                programCode,
                trainingLevel,
                isActive,
                page - 1,
                pageSize,
                sort));
        return ResponseEntity.ok(mapper.toPageResponse(result));
    }
}
