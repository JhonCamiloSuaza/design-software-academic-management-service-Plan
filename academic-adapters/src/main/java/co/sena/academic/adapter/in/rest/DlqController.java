package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.event.AcademicDlqReprocessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Explicit operator entry point for R1 DLQ recovery. */
@RestController
@RequestMapping("/api/v1/events/dlq")
public class DlqController {
    private final AcademicDlqReprocessor reprocessor;
    public DlqController(AcademicDlqReprocessor reprocessor) { this.reprocessor = reprocessor; }

    @PostMapping("/reprocess-next")
    @PreAuthorize("hasAuthority('ACADEMIC_FICHA_MANAGE')")
    public ResponseEntity<AcademicDlqReprocessor.ReprocessResult> reprocessNext() {
        return ResponseEntity.ok(reprocessor.reprocessNext());
    }
}
