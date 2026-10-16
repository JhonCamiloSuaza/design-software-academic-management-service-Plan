package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.rest.dto.CreateFichaRequest;
import co.sena.academic.adapter.in.rest.dto.ChangeFichaStatusRequest;
import co.sena.academic.adapter.in.rest.dto.FichaPageResponse;
import co.sena.academic.adapter.in.rest.dto.FichaResponse;
import co.sena.academic.adapter.in.rest.dto.UpdateFichaRequest;
import co.sena.academic.adapter.in.rest.mapper.FichaRestMapper;
import co.sena.academic.adapter.in.rest.security.FichaAccessContextResolver;
import co.sena.academic.application.port.in.ChangeFichaStatusUseCase;
import co.sena.academic.application.port.in.CreateFichaUseCase;
import co.sena.academic.application.port.in.QueryFichasUseCase;
import co.sena.academic.application.port.in.UpdateFichaUseCase;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fichas")
@Validated
@Observed(name = "ficha.controller")
public class FichaController {

    private final CreateFichaUseCase createFichaUseCase;
    private final ChangeFichaStatusUseCase changeFichaStatusUseCase;
    private final QueryFichasUseCase queryFichasUseCase;
    private final UpdateFichaUseCase updateFichaUseCase;
    private final FichaRestMapper mapper;
    private final FichaAccessContextResolver accessContextResolver;

    public FichaController(CreateFichaUseCase createFichaUseCase,
                           ChangeFichaStatusUseCase changeFichaStatusUseCase,
                           QueryFichasUseCase queryFichasUseCase,
                           UpdateFichaUseCase updateFichaUseCase,
                           FichaRestMapper mapper,
                           FichaAccessContextResolver accessContextResolver) {
        this.createFichaUseCase = createFichaUseCase;
        this.changeFichaStatusUseCase = changeFichaStatusUseCase;
        this.queryFichasUseCase = queryFichasUseCase;
        this.updateFichaUseCase = updateFichaUseCase;
        this.mapper = mapper;
        this.accessContextResolver = accessContextResolver;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACADEMIC_FICHA_MANAGE')")
    @Transactional
    public ResponseEntity<FichaResponse> create(
            @Valid @RequestBody CreateFichaRequest request,
            @AuthenticationPrincipal UUID actorId) {
        var created = createFichaUseCase.create(new CreateFichaUseCase.Command(
                request.fichaNumber(),
                request.programId(),
                request.trainingCenterId(),
                request.startDate(),
                request.expectedEndDate(),
                request.trainingShift(),
                request.trainingModality(),
                request.maxCapacity(),
                actorId));
        FichaResponse body = mapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/fichas/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ACADEMIC_FICHA_VIEW', 'ACADEMIC_FICHA_VIEW_OWN')")
    public ResponseEntity<FichaResponse> getById(
            @PathVariable UUID id,
            Authentication authentication) {
        var access = accessContextResolver.resolve(authentication);
        return ResponseEntity.ok(mapper.toResponse(queryFichasUseCase.getById(id, access)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_FICHA_MANAGE')")
    @Transactional
    public ResponseEntity<FichaResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFichaRequest request,
            Authentication authentication) {
        var access = accessContextResolver.resolve(authentication, "ACADEMIC_FICHA_MANAGE");
        var updated = updateFichaUseCase.update(new UpdateFichaUseCase.Command(
                id,
                request.trainingShift(),
                request.trainingShiftPresent(),
                request.trainingModality(),
                request.trainingModalityPresent(),
                request.expectedEndDate(),
                request.expectedEndDatePresent(),
                request.maxCapacity(),
                request.maxCapacityPresent(),
                request.rowVersion(),
                access));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ACADEMIC_FICHA_STATUS')")
    @Transactional
    public ResponseEntity<FichaResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeFichaStatusRequest request,
            Authentication authentication) {
        var access = accessContextResolver.resolve(authentication, "ACADEMIC_FICHA_STATUS");
        var changed = changeFichaStatusUseCase.changeStatus(new ChangeFichaStatusUseCase.Command(
                id,
                request.status(),
                request.actualEndDate(),
                access,
                hasRole(authentication, "CENTER_DIRECTOR")));
        return ResponseEntity.ok(mapper.toResponse(changed));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_FICHA_STATUS')")
    @Transactional
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication authentication) {
        var access = accessContextResolver.resolve(authentication, "ACADEMIC_FICHA_STATUS");
        changeFichaStatusUseCase.changeStatus(new ChangeFichaStatusUseCase.Command(
                id,
                "CANCELLED",
                LocalDate.now(),
                access,
                hasRole(authentication, "CENTER_DIRECTOR")));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ACADEMIC_FICHA_VIEW', 'ACADEMIC_FICHA_VIEW_OWN')")
    public ResponseEntity<FichaPageResponse> list(
            @RequestParam(value = "program_id", required = false) UUID programId,
            @RequestParam(value = "status", required = false)
            @Pattern(
                    regexp = "INDUCTION|EXECUTION|PRODUCTIVE_STAGE|COMPLETED|CANCELLED",
                    message = "status must be a valid ficha status")
            String status,
            @RequestParam(value = "ficha_number", required = false) String fichaNumber,
            @RequestParam(value = "training_center_id", required = false) UUID trainingCenterId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "1")
            @Min(value = 1, message = "page must be >= 1") int page,
            @RequestParam(name = "page_size", defaultValue = "20")
            @Min(value = 1, message = "page_size must be >= 1")
            @Max(value = 100, message = "page_size must be <= 100") int pageSize,
            @RequestParam(required = false) String sort,
            Authentication authentication) {
        var access = accessContextResolver.resolve(authentication);
        var result = queryFichasUseCase.list(new QueryFichasUseCase.Query(
                programId, status, fichaNumber, trainingCenterId, from, to,
                page - 1, pageSize, sort, access));
        return ResponseEntity.ok(mapper.toPageResponse(result));
    }

    private boolean hasRole(Authentication authentication, String expectedRole) {
        Jwt jwt = null;
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            jwt = jwtAuthentication.getToken();
        } else if (authentication != null && authentication.getCredentials() instanceof Jwt credentials) {
            jwt = credentials;
        }
        if (jwt == null) {
            return false;
        }
        var roles = jwt.getClaimAsStringList("roles");
        if (roles != null && roles.contains(expectedRole)) {
            return true;
        }
        String role = jwt.getClaimAsString("role");
        return expectedRole.equals(role);
    }
}
