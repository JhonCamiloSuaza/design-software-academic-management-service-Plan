package co.sena.academic.application.usecase;

import co.sena.academic.application.port.in.QueryFichaReportsUseCase;
import co.sena.academic.application.port.in.QueryFichasUseCase;
import co.sena.academic.application.port.out.FichaReportRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FichaReportServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void calculatesDelayedOnTimeAndUnknownExpectedEndDate() {
        UUID centerId = UUID.randomUUID();
        StubReportRepository repository = new StubReportRepository(List.of(
                progress("F-DELAYED", "EXECUTION", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)),
                progress("F-ON-TIME", "EXECUTION", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1)),
                progress("F-NO-EXPECTED", "EXECUTION", LocalDate.of(2026, 10, 5), null),
                progress("F-CLOSED", "COMPLETED", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 2))));
        FichaReportService service = new FichaReportService(repository, CLOCK);

        QueryFichaReportsUseCase.FichaProgressPage result = service.fichaProgress(query(centerId));

        assertThat(result.content())
                .extracting(QueryFichaReportsUseCase.FichaProgressItem::fichaNumber,
                        QueryFichaReportsUseCase.FichaProgressItem::daysElapsed,
                        QueryFichaReportsUseCase.FichaProgressItem::daysRemaining,
                        QueryFichaReportsUseCase.FichaProgressItem::delayed)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("F-DELAYED", 43, -13, true),
                        org.assertj.core.groups.Tuple.tuple("F-ON-TIME", 13, 18, false),
                        org.assertj.core.groups.Tuple.tuple("F-NO-EXPECTED", 9, null, false),
                        org.assertj.core.groups.Tuple.tuple("F-CLOSED", 31, null, false));
    }

    @Test
    void trainingCenterScopeCannotQueryAnotherCenter() {
        StubReportRepository repository = new StubReportRepository(List.of());
        FichaReportService service = new FichaReportService(repository, CLOCK);
        UUID ownCenter = UUID.randomUUID();
        UUID otherCenter = UUID.randomUUID();

        QueryFichaReportsUseCase.EnrollmentByFichaPage result = service.enrollmentByFicha(
                new QueryFichaReportsUseCase.Query(
                        otherCenter, null, null, null, null, 0, 20, null,
                        new QueryFichasUseCase.AccessContext(
                                UUID.randomUUID(), null, ownCenter,
                                QueryFichasUseCase.ScopeType.TRAINING_CENTER)));

        assertThat(result.content()).isEmpty();
        assertThat(repository.queryWasExecuted).isFalse();
    }

    private QueryFichaReportsUseCase.Query query(UUID centerId) {
        return new QueryFichaReportsUseCase.Query(
                centerId, null, null, null, null, 0, 20, null,
                new QueryFichasUseCase.AccessContext(
                        UUID.randomUUID(), null, null, QueryFichasUseCase.ScopeType.GLOBAL));
    }

    private FichaReportRepositoryPort.ProgressSource progress(String number,
                                                               String status,
                                                               LocalDate start,
                                                               LocalDate expected) {
        return progress(number, status, start, expected, null);
    }

    private FichaReportRepositoryPort.ProgressSource progress(String number,
                                                               String status,
                                                               LocalDate start,
                                                               LocalDate expected,
                                                               LocalDate actual) {
        return new FichaReportRepositoryPort.ProgressSource(
                UUID.randomUUID(), number, status, start, expected, actual);
    }

    private static final class StubReportRepository implements FichaReportRepositoryPort {
        private final List<ProgressSource> progressSources;
        private boolean queryWasExecuted;

        private StubReportRepository(List<ProgressSource> progressSources) {
            this.progressSources = progressSources;
        }

        @Override
        public List<QueryFichaReportsUseCase.EnrollmentByFichaItem> findEnrollmentByFicha(
                Filter filter, int page, int size, Optional<String> sort) {
            queryWasExecuted = true;
            return List.of();
        }

        @Override
        public long countEnrollmentByFicha(Filter filter) {
            queryWasExecuted = true;
            return 0;
        }

        @Override
        public List<ProgressSource> findFichaProgress(
                Filter filter, int page, int size, Optional<String> sort) {
            queryWasExecuted = true;
            return progressSources;
        }

        @Override
        public long countFichaProgress(Filter filter) {
            queryWasExecuted = true;
            return progressSources.size();
        }
    }
}
