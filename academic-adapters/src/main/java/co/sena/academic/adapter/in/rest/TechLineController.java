package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.rest.dto.CreateTechLineRequest;
import co.sena.academic.adapter.in.rest.dto.TechLinePageResponse;
import co.sena.academic.adapter.in.rest.dto.TechLineResponse;
import co.sena.academic.adapter.in.rest.dto.UpdateTechLineRequest;
import co.sena.academic.adapter.in.rest.mapper.TechLineRestMapper;
import co.sena.academic.application.port.in.CreateTechLineUseCase;
import co.sena.academic.application.port.in.DeactivateTechLineUseCase;
import co.sena.academic.application.port.in.QueryTechLinesUseCase;
import co.sena.academic.application.port.in.UpdateTechLineUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * HU-ACAD-01: tech-lines. Four endpoints = four Gherkin criteria (E1-E4).
 *
 * NOTE: @AuthenticationPrincipal UUID assumes the resource server already
 * resolves the JWT into an actor UUID (sub) via a custom converter/principal,
 * as is done elsewhere in the system. If your project uses a different
 * Principal type, replace it here and in the mappers.
 */
@RestController
@RequestMapping("/tech-lines")
public class TechLineController {

    private final CreateTechLineUseCase createTechLineUseCase;
    private final QueryTechLinesUseCase queryTechLinesUseCase;
    private final UpdateTechLineUseCase updateTechLineUseCase;
    private final DeactivateTechLineUseCase deactivateTechLineUseCase;
    private final TechLineRestMapper mapper;

    public TechLineController(CreateTechLineUseCase createTechLineUseCase,
                               QueryTechLinesUseCase queryTechLinesUseCase,
                               UpdateTechLineUseCase updateTechLineUseCase,
                               DeactivateTechLineUseCase deactivateTechLineUseCase,
                               TechLineRestMapper mapper) {
        this.createTechLineUseCase = createTechLineUseCase;
        this.queryTechLinesUseCase = queryTechLinesUseCase;
        this.updateTechLineUseCase = updateTechLineUseCase;
        this.deactivateTechLineUseCase = deactivateTechLineUseCase;
        this.mapper = mapper;
    }

    // ---- E1 ----
    @PostMapping
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<TechLineResponse> create(@Valid @RequestBody CreateTechLineRequest request,
                                                     @AuthenticationPrincipal UUID actorId) {
        var created = createTechLineUseCase.create(
                new CreateTechLineUseCase.Command(request.name(), request.code(), actorId));
        TechLineResponse body = mapper.toResponse(created);
        return ResponseEntity.created(URI.create("/tech-lines/" + body.id())).body(body);
    }

    // ---- E2 ----
    @GetMapping
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_VIEW')")
    public ResponseEntity<TechLinePageResponse> list(
            @RequestParam(value = "is_active", required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = queryTechLinesUseCase.list(
                new QueryTechLinesUseCase.Query(Optional.ofNullable(isActive), page, size));
        return ResponseEntity.ok(mapper.toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_VIEW')")
    public ResponseEntity<TechLineResponse> getById(@PathVariable UUID id) {
        var result = queryTechLinesUseCase.getById(id);
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    // ---- E3 ----
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<TechLineResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateTechLineRequest request,
                                                     @AuthenticationPrincipal UUID actorId) {
        var updated = updateTechLineUseCase.update(
                new UpdateTechLineUseCase.Command(id, request.name(), request.code(), actorId));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    // ---- E4 ----
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id,
                                             @AuthenticationPrincipal UUID actorId) {
        deactivateTechLineUseCase.deactivate(id, actorId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}