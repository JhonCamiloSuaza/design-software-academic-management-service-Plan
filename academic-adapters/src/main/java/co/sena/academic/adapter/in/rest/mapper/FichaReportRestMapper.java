package co.sena.academic.adapter.in.rest.mapper;

import co.sena.academic.adapter.in.rest.dto.EnrollmentByFichaReportItemResponse;
import co.sena.academic.adapter.in.rest.dto.EnrollmentByFichaReportPageResponse;
import co.sena.academic.adapter.in.rest.dto.FichaProgressReportItemResponse;
import co.sena.academic.adapter.in.rest.dto.FichaProgressReportPageResponse;
import co.sena.academic.application.port.in.QueryFichaReportsUseCase;
import org.springframework.stereotype.Component;

@Component
public class FichaReportRestMapper {

    public EnrollmentByFichaReportPageResponse toEnrollmentPage(
            QueryFichaReportsUseCase.EnrollmentByFichaPage page) {
        return new EnrollmentByFichaReportPageResponse(
                page.content().stream().map(this::toEnrollmentItem).toList(),
                new EnrollmentByFichaReportPageResponse.Pagination(
                        page.page() + 1,
                        page.size(),
                        page.totalElements(),
                        totalPages(page.totalElements(), page.size())));
    }

    public FichaProgressReportPageResponse toProgressPage(
            QueryFichaReportsUseCase.FichaProgressPage page) {
        return new FichaProgressReportPageResponse(
                page.content().stream().map(this::toProgressItem).toList(),
                new FichaProgressReportPageResponse.Pagination(
                        page.page() + 1,
                        page.size(),
                        page.totalElements(),
                        totalPages(page.totalElements(), page.size())));
    }

    private EnrollmentByFichaReportItemResponse toEnrollmentItem(
            QueryFichaReportsUseCase.EnrollmentByFichaItem item) {
        return new EnrollmentByFichaReportItemResponse(
                item.fichaId(),
                item.fichaNumber(),
                item.programId(),
                item.programCode(),
                item.programName(),
                item.trainingCenterId(),
                item.status(),
                item.maxCapacity(),
                item.trainingShift(),
                item.trainingModality(),
                item.startDate());
    }

    private FichaProgressReportItemResponse toProgressItem(
            QueryFichaReportsUseCase.FichaProgressItem item) {
        return new FichaProgressReportItemResponse(
                item.fichaId(),
                item.fichaNumber(),
                item.status(),
                item.startDate(),
                item.expectedEndDate(),
                item.actualEndDate(),
                item.daysElapsed(),
                item.daysRemaining(),
                item.delayed());
    }

    private int totalPages(long totalElements, int size) {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
