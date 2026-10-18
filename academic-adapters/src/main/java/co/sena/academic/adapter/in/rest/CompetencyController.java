package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.rest.dto.CompetencyPageResponse;
import co.sena.academic.adapter.in.rest.dto.CompetencyResponse;
import co.sena.academic.adapter.in.rest.dto.CreateCompetencyRequest;
import co.sena.academic.adapter.in.rest.dto.UpdateCompetencyRequest;
import co.sena.academic.adapter.in.rest.mapper.CompetencyRestMapper;
import co.sena.academic.application.port.in.CreateCompetencyUseCase;
import co.sena.academic.application.port.in.DeactivateCompetencyUseCase;
import co.sena.academic.application.port.in.QueryCompetenciesUseCase;
import co.sena.academic.application.port.in.UpdateCompetencyUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping
@Validated
public class CompetencyController {

    private final CreateCompetencyUseCase createUseCase;
    private final UpdateCompetencyUseCase updateUseCase;
    private final DeactivateCompetencyUseCase deactivateUseCase;
    private final QueryCompetenciesUseCase queryUseCase;
    private final CompetencyRestMapper mapper;

    public CompetencyController(CreateCompetencyUseCase createUseCase,
                                UpdateCompetencyUseCase updateUseCase,
                                DeactivateCompetencyUseCase deactivateUseCase,
                                QueryCompetenciesUseCase queryUseCase,
                                CompetencyRestMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deactivateUseCase = deactivateUseCase;
        this.queryUseCase = queryUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/training-programs/{programId}/competencies")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<CompetencyResponse> create(
            @PathVariable UUID programId,
            @Valid @RequestBody CreateCompetencyRequest request,
            @AuthenticationPrincipal UUID actorId) {
        var created = createUseCase.create(new CreateCompetencyUseCase.Command(
                programId,
                request.name(),
                request.senaCode(),
                request.hours(),
                request.isActive(),
                actorId));
        var body = mapper.toResponse(created);
        return ResponseEntity.created(URI.create("/competencies/" + body.id())).body(body);
    }

    @GetMapping("/training-programs/{programId}/competencies")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_VIEW')")
    public ResponseEntity<CompetencyPageResponse> list(
            @PathVariable UUID programId,
            @RequestParam(value = "is_active", required = false) Boolean isActive,
            @RequestParam(name = "page", defaultValue = "1")
            @Min(value = 1, message = "page must be >= 1") int page,
            @RequestParam(name = "page_size", defaultValue = "20")
            @Min(value = 1, message = "page_size must be >= 1")
            @Max(value = 100, message = "page_size must be <= 100") int pageSize,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(mapper.toPageResponse(queryUseCase.list(
                new QueryCompetenciesUseCase.Query(
                        programId,
                        Optional.ofNullable(isActive),
                        page - 1,
                        pageSize,
                        Optional.ofNullable(sort)))));
    }

    @GetMapping("/competencies/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_VIEW')")
    public ResponseEntity<CompetencyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(queryUseCase.getById(id)));
    }

    @PutMapping("/competencies/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<CompetencyResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompetencyRequest request,
            @AuthenticationPrincipal UUID actorId) {
        var updated = updateUseCase.update(new UpdateCompetencyUseCase.Command(
                id,
                request.name(),
                request.senaCode(),
                request.hours(),
                request.isActive(),
                request.rowVersion(),
                actorId));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/competencies/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID actorId) {
        deactivateUseCase.deactivate(new DeactivateCompetencyUseCase.Command(id, actorId));
        return ResponseEntity.noContent().build();
    }
}
