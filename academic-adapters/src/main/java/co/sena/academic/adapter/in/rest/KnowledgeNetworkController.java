package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.rest.dto.CreateKnowledgeNetworkRequest;
import co.sena.academic.adapter.in.rest.dto.KnowledgeNetworkPageResponse;
import co.sena.academic.adapter.in.rest.dto.KnowledgeNetworkResponse;
import co.sena.academic.adapter.in.rest.dto.UpdateKnowledgeNetworkRequest;
import co.sena.academic.adapter.in.rest.mapper.KnowledgeNetworkRestMapper;
import co.sena.academic.application.port.in.CreateKnowledgeNetworkUseCase;
import co.sena.academic.application.port.in.DeactivateKnowledgeNetworkUseCase;
import co.sena.academic.application.port.in.QueryKnowledgeNetworksUseCase;
import co.sena.academic.application.port.in.UpdateKnowledgeNetworkUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
// imports nuevos
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * HU-ACAD-03: knowledge-networks (third level of the curricular hierarchy).
 * E1 (create), E2 (filtered list/get), E3 (baja blocked by active
 * training_program dependents), plus update for CRUD symmetry with
 * HU-ACAD-01 (tech_line) — see UpdateKnowledgeNetworkUseCase's javadoc.
 *
 * NOTE: @AuthenticationPrincipal UUID assumes the resource server already
 * resolves the JWT into an actor UUID (sub) via JwtFeaturesConverter, same
 * as TechLineController.
 */
@RestController
@RequestMapping("/knowledge-networks")
@Validated   
public class KnowledgeNetworkController {

    private final CreateKnowledgeNetworkUseCase createKnowledgeNetworkUseCase;
    private final QueryKnowledgeNetworksUseCase queryKnowledgeNetworksUseCase;
    private final UpdateKnowledgeNetworkUseCase updateKnowledgeNetworkUseCase;
    private final DeactivateKnowledgeNetworkUseCase deactivateKnowledgeNetworkUseCase;
    private final KnowledgeNetworkRestMapper mapper;

    public KnowledgeNetworkController(CreateKnowledgeNetworkUseCase createKnowledgeNetworkUseCase,
                                       QueryKnowledgeNetworksUseCase queryKnowledgeNetworksUseCase,
                                       UpdateKnowledgeNetworkUseCase updateKnowledgeNetworkUseCase,
                                       DeactivateKnowledgeNetworkUseCase deactivateKnowledgeNetworkUseCase,
                                       KnowledgeNetworkRestMapper mapper) {
        this.createKnowledgeNetworkUseCase = createKnowledgeNetworkUseCase;
        this.queryKnowledgeNetworksUseCase = queryKnowledgeNetworksUseCase;
        this.updateKnowledgeNetworkUseCase = updateKnowledgeNetworkUseCase;
        this.deactivateKnowledgeNetworkUseCase = deactivateKnowledgeNetworkUseCase;
        this.mapper = mapper;
    }

    // ---- E1 ----
    @PostMapping
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<KnowledgeNetworkResponse> create(@Valid @RequestBody CreateKnowledgeNetworkRequest request,
                                                             @AuthenticationPrincipal UUID actorId) {
        var created = createKnowledgeNetworkUseCase.create(
                new CreateKnowledgeNetworkUseCase.Command(
                        request.techNetworkId(), request.name(), request.code(), actorId));
        KnowledgeNetworkResponse body = mapper.toResponse(created);
        return ResponseEntity.created(URI.create("/knowledge-networks/" + body.id())).body(body);
    }

    // ---- E2 ----
    // tech_network_id is REQUIRED (see QueryKnowledgeNetworksUseCase javadoc): a missing
    // param is rejected with 400 by KnowledgeNetworkExceptionHandler (MissingServletRequestParameterException).
    @GetMapping
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_VIEW')")
    public ResponseEntity<KnowledgeNetworkPageResponse> list(
            @RequestParam(value = "tech_network_id", required = false) UUID techNetworkId,
            @RequestParam(value = "is_active", required = false) Boolean isActive,
            @RequestParam(name = "page", defaultValue = "1") @Min(value = 1, message = "page must be >= 1") int page,
            @RequestParam(name = "page_size", defaultValue = "20")
            @Min(value = 1, message = "page_size must be >= 1")
            @Max(value = 100, message = "page_size must be <= 100") int pageSize,
            @RequestParam(required = false) String sort) {
        int zeroBasedPage = page - 1;   // ya no hace falta Math.max: page<1 ahora se rechaza antes de llegar aquí
        var result = queryKnowledgeNetworksUseCase.list(
                new QueryKnowledgeNetworksUseCase.Query(
                        techNetworkId, Optional.ofNullable(isActive), zeroBasedPage, pageSize, sort));
        return ResponseEntity.ok(mapper.toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_VIEW')")
    public ResponseEntity<KnowledgeNetworkResponse> getById(@PathVariable UUID id) {
        var result = queryKnowledgeNetworksUseCase.getById(id);
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    // ---- update ----
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<KnowledgeNetworkResponse> update(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateKnowledgeNetworkRequest request,
                                                            @AuthenticationPrincipal UUID actorId) {
        var updated = updateKnowledgeNetworkUseCase.update(
                new UpdateKnowledgeNetworkUseCase.Command(
                        id, request.techNetworkId(), request.name(), request.code(), request.isActive(), actorId));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    // ---- E3 ----
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id,
                                            @AuthenticationPrincipal UUID actorId) {
        deactivateKnowledgeNetworkUseCase.deactivate(id, actorId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}