package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.rest.dto.CreateTechNetworkRequest;
import co.sena.academic.adapter.in.rest.dto.TechNetworkPageResponse;
import co.sena.academic.adapter.in.rest.dto.TechNetworkResponse;
import co.sena.academic.adapter.in.rest.dto.UpdateTechNetworkRequest;
import co.sena.academic.adapter.in.rest.mapper.TechNetworkRestMapper;
import co.sena.academic.application.port.in.CreateTechNetworkUseCase;
import co.sena.academic.application.port.in.DeactivateTechNetworkUseCase;
import co.sena.academic.application.port.in.QueryTechNetworksUseCase;
import co.sena.academic.application.port.in.UpdateTechNetworkUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/** HU-ACAD-02: tech-networks. Same conventions as TechLineController (HU-ACAD-01). */
@RestController
@RequestMapping("/tech-networks")
public class TechNetworkController {

    private final CreateTechNetworkUseCase createTechNetworkUseCase;
    private final QueryTechNetworksUseCase queryTechNetworksUseCase;
    private final UpdateTechNetworkUseCase updateTechNetworkUseCase;
    private final DeactivateTechNetworkUseCase deactivateTechNetworkUseCase;
    private final TechNetworkRestMapper mapper;

    public TechNetworkController(CreateTechNetworkUseCase createTechNetworkUseCase,
                                  QueryTechNetworksUseCase queryTechNetworksUseCase,
                                  UpdateTechNetworkUseCase updateTechNetworkUseCase,
                                  DeactivateTechNetworkUseCase deactivateTechNetworkUseCase,
                                  TechNetworkRestMapper mapper) {
        this.createTechNetworkUseCase = createTechNetworkUseCase;
        this.queryTechNetworksUseCase = queryTechNetworksUseCase;
        this.updateTechNetworkUseCase = updateTechNetworkUseCase;
        this.deactivateTechNetworkUseCase = deactivateTechNetworkUseCase;
        this.mapper = mapper;
    }

    // ---- E1 ----
    @PostMapping
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<TechNetworkResponse> create(@Valid @RequestBody CreateTechNetworkRequest request,
                                                        @AuthenticationPrincipal UUID actorId) {
        var created = createTechNetworkUseCase.create(
                new CreateTechNetworkUseCase.Command(request.techLineId(), request.name(), request.code(), actorId));
        TechNetworkResponse body = mapper.toResponse(created);
        return ResponseEntity.created(URI.create("/tech-networks/" + body.id())).body(body);
    }

    // ---- E2 ----
    @GetMapping
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_VIEW')")
    public ResponseEntity<TechNetworkPageResponse> list(
            @RequestParam(value = "tech_line_id", required = false) UUID techLineId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = queryTechNetworksUseCase.list(
                new QueryTechNetworksUseCase.Query(Optional.ofNullable(techLineId), page, size));
        return ResponseEntity.ok(mapper.toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_VIEW')")
    public ResponseEntity<TechNetworkResponse> getById(@PathVariable UUID id) {
        var result = queryTechNetworksUseCase.getById(id);
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    // ---- E3 ----
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<TechNetworkResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateTechNetworkRequest request,
                                                        @AuthenticationPrincipal UUID actorId) {
        var updated = updateTechNetworkUseCase.update(
                new UpdateTechNetworkUseCase.Command(id, request.name(), request.code(), actorId));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_PROGRAM_MANAGE')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id,
                                            @AuthenticationPrincipal UUID actorId) {
        deactivateTechNetworkUseCase.deactivate(id, actorId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}