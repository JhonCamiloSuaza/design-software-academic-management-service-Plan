package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.rest.dto.EnrollmentByFichaReportPageResponse;
import co.sena.academic.adapter.in.rest.dto.FichaProgressReportPageResponse;
import co.sena.academic.adapter.in.rest.mapper.FichaReportRestMapper;
import co.sena.academic.adapter.in.rest.security.FichaAccessContextResolver;
import co.sena.academic.application.port.in.QueryFichaReportsUseCase;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Validated
@Observed(name = "ficha.reports.controller")
public class FichaReportController {

    private static final String FICHA_STATUS_PATTERN =
            "INDUCTION|EXECUTION|PRODUCTIVE_STAGE|COMPLETED|CANCELLED";

    private final QueryFichaReportsUseCase reportUseCase;
    private final FichaReportRestMapper mapper;
    private final FichaAccessContextResolver accessContextResolver;

    public FichaReportController(QueryFichaReportsUseCase reportUseCase,
                                 FichaReportRestMapper mapper,
                                 FichaAccessContextResolver accessContextResolver) {
        this.reportUseCase = reportUseCase;
        this.mapper = mapper;
        this.accessContextResolver = accessContextResolver;
    }

    @GetMapping("/enrollment-by-ficha")
    @PreAuthorize("hasAuthority('ACADEMIC_FICHA_VIEW')")
    public ResponseEntity<EnrollmentByFichaReportPageResponse> enrollmentByFicha(
            @RequestParam(value = "training_center_id", required = false) UUID trainingCenterId,
            @RequestParam(value = "program_id", required = false) UUID programId,
            @RequestParam(value = "status", required = false)
            @Pattern(regexp = FICHA_STATUS_PATTERN, message = "status must be a valid ficha status")
            String status,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "page_size", defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String sort,
            Authentication authentication) {
        var result = reportUseCase.enrollmentByFicha(query(
                trainingCenterId, programId, status, from, to, page, pageSize, sort, authentication));
        return ResponseEntity.ok(mapper.toEnrollmentPage(result));
    }

    @GetMapping("/ficha-progress")
    @PreAuthorize("hasAuthority('ACADEMIC_FICHA_VIEW')")
    public ResponseEntity<FichaProgressReportPageResponse> fichaProgress(
            @RequestParam(value = "training_center_id", required = false) UUID trainingCenterId,
            @RequestParam(value = "program_id", required = false) UUID programId,
            @RequestParam(value = "status", required = false)
            @Pattern(regexp = FICHA_STATUS_PATTERN, message = "status must be a valid ficha status")
            String status,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "page_size", defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String sort,
            Authentication authentication) {
        var result = reportUseCase.fichaProgress(query(
                trainingCenterId, programId, status, from, to, page, pageSize, sort, authentication));
        return ResponseEntity.ok(mapper.toProgressPage(result));
    }

    private QueryFichaReportsUseCase.Query query(UUID trainingCenterId,
                                                  UUID programId,
                                                  String status,
                                                  LocalDate from,
                                                  LocalDate to,
                                                  int page,
                                                  int pageSize,
                                                  String sort,
                                                  Authentication authentication) {
        return new QueryFichaReportsUseCase.Query(
                trainingCenterId,
                programId,
                status,
                from,
                to,
                page - 1,
                pageSize,
                sort,
                accessContextResolver.resolve(authentication));
    }
}
