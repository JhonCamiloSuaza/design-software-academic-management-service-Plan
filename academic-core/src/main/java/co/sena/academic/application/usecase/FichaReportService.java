package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.QueryFichaReportsUseCase;
import co.sena.academic.application.port.in.QueryFichasUseCase;
import co.sena.academic.application.port.out.FichaReportRepositoryPort;
import co.sena.academic.domain.exception.FichaScopeAccessDeniedException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Application service for read-only ficha reports. */
public class FichaReportService implements QueryFichaReportsUseCase {

    private final FichaReportRepositoryPort reportRepository;
    private final Clock clock;

    public FichaReportService(FichaReportRepositoryPort reportRepository) {
        this(reportRepository, Clock.systemDefaultZone());
    }

    FichaReportService(FichaReportRepositoryPort reportRepository, Clock clock) {
        this.reportRepository = reportRepository;
        this.clock = clock;
    }

    @Override
    public EnrollmentByFichaPage enrollmentByFicha(Query query) {
        FichaReportRepositoryPort.Filter filter = effectiveFilter(query);
        if (filter == null) {
            return new EnrollmentByFichaPage(List.of(), 0, query.page(), query.size());
        }
        List<EnrollmentByFichaItem> content = reportRepository.findEnrollmentByFicha(
                filter, query.page(), query.size(), query.sort());
        return new EnrollmentByFichaPage(
                content,
                reportRepository.countEnrollmentByFicha(filter),
                query.page(),
                query.size());
    }

    @Override
    public FichaProgressPage fichaProgress(Query query) {
        FichaReportRepositoryPort.Filter filter = effectiveFilter(query);
        if (filter == null) {
            return new FichaProgressPage(List.of(), 0, query.page(), query.size());
        }

        LocalDate today = LocalDate.now(clock);
        List<FichaProgressItem> content = reportRepository.findFichaProgress(
                        filter, query.page(), query.size(), query.sort())
                .stream()
                .map(source -> toProgressItem(source, today))
                .toList();
        return new FichaProgressPage(
                content,
                reportRepository.countFichaProgress(filter),
                query.page(),
                query.size());
    }

    private FichaReportRepositoryPort.Filter effectiveFilter(Query query) {
        QueryFichasUseCase.AccessContext access = query.access();
        if (access == null || access.scope() == null) {
            throw new FichaScopeAccessDeniedException("A ficha scope is required");
        }
        if (access.scope() != QueryFichasUseCase.ScopeType.GLOBAL
                && access.scope() != QueryFichasUseCase.ScopeType.TRAINING_CENTER) {
            throw new FichaScopeAccessDeniedException(
                    "Reports support GLOBAL and TRAINING_CENTER scopes only");
        }
        if (access.scope() == QueryFichasUseCase.ScopeType.TRAINING_CENTER) {
            if (access.trainingCenterId() == null) {
                throw new FichaScopeAccessDeniedException(
                        "training_center_id is required for TRAINING_CENTER");
            }
            if (query.trainingCenterId().isPresent()
                    && !query.trainingCenterId().get().equals(access.trainingCenterId())) {
                return null;
            }
        }

        Optional<UUID> trainingCenter = access.scope() == QueryFichasUseCase.ScopeType.TRAINING_CENTER
                ? Optional.of(access.trainingCenterId())
                : query.trainingCenterId();
        return new FichaReportRepositoryPort.Filter(
                trainingCenter, query.programId(), query.status(), query.from(), query.to());
    }

    private FichaProgressItem toProgressItem(FichaReportRepositoryPort.ProgressSource source,
                                             LocalDate today) {
        LocalDate elapsedUntil = source.actualEndDate() != null
                ? source.actualEndDate()
                : today;
        int daysElapsed = saturatedDaysBetween(source.startDate(), elapsedUntil, true);
        boolean terminal = "COMPLETED".equals(source.status()) || "CANCELLED".equals(source.status());
        boolean delayed = source.expectedEndDate() != null
                && today.isAfter(source.expectedEndDate())
                && !terminal;
        Integer daysRemaining = source.expectedEndDate() == null || terminal
                ? null
                : saturatedDaysBetween(today, source.expectedEndDate(), false);

        return new FichaProgressItem(
                source.fichaId(),
                source.fichaNumber(),
                source.status(),
                source.startDate(),
                source.expectedEndDate(),
                source.actualEndDate(),
                daysElapsed,
                daysRemaining,
                delayed);
    }

    private int saturatedDaysBetween(LocalDate start, LocalDate end, boolean nonNegative) {
        long days = ChronoUnit.DAYS.between(start, end);
        if (nonNegative) {
            days = Math.max(0, days);
        }
        return days > Integer.MAX_VALUE ? Integer.MAX_VALUE
                : days < Integer.MIN_VALUE ? Integer.MIN_VALUE
                : (int) days;
    }
}
