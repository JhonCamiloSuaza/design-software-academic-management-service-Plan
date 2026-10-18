package co.sena.academic.adapter.out.persistence;

import co.sena.academic.application.port.in.QueryFichaReportsUseCase;
import co.sena.academic.application.port.out.FichaReportRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** PostgreSQL read projections for ficha reports. */
@Component
@Transactional(readOnly = true)
@Observed(name = "ficha.reports.persistence")
public class FichaReportPersistenceAdapter implements FichaReportRepositoryPort {

    private static final String FROM_FICHAS = " FROM academic_management.enrollment_ficha f ";
    private static final String ENROLLMENT_SELECT = "SELECT f.id, f.ficha_number, f.program_id, "
            + "p.program_code, p.name, f.training_center_id, f.status, f.max_capacity, "
            + "f.training_shift, f.training_modality, f.start_date "
            + FROM_FICHAS
            + "JOIN academic_management.training_program p ON p.id = f.program_id";
    private static final String PROGRESS_SELECT = "SELECT f.id, f.ficha_number, f.status, f.start_date, "
            + "f.expected_end_date, f.actual_end_date "
            + FROM_FICHAS;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<QueryFichaReportsUseCase.EnrollmentByFichaItem> findEnrollmentByFicha(
            Filter filter, int page, int size, Optional<String> sort) {
        Query query = entityManager.createNativeQuery(
                ENROLLMENT_SELECT + whereClause(filter) + orderByEnrollment(sort));
        bind(query, filter);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return rows(query).stream().map(this::toEnrollmentItem).toList();
    }

    @Override
    public long countEnrollmentByFicha(Filter filter) {
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*)" + FROM_FICHAS
                        + "JOIN academic_management.training_program p ON p.id = f.program_id"
                        + whereClause(filter));
        bind(query, filter);
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<ProgressSource> findFichaProgress(Filter filter,
                                                   int page,
                                                   int size,
                                                   Optional<String> sort) {
        Query query = entityManager.createNativeQuery(
                PROGRESS_SELECT + whereClause(filter) + orderByProgress(sort));
        bind(query, filter);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return rows(query).stream().map(this::toProgressSource).toList();
    }

    @Override
    public long countFichaProgress(Filter filter) {
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*)" + FROM_FICHAS + whereClause(filter));
        bind(query, filter);
        return ((Number) query.getSingleResult()).longValue();
    }

    private String whereClause(Filter filter) {
        List<String> predicates = new ArrayList<>();
        if (filter.trainingCenterId().isPresent()) {
            predicates.add("f.training_center_id = :trainingCenterId");
        }
        if (filter.programId().isPresent()) {
            predicates.add("f.program_id = :programId");
        }
        if (filter.status().isPresent()) {
            predicates.add("f.status = :status");
        }
        if (filter.from().isPresent()) {
            predicates.add("f.start_date >= :from");
        }
        if (filter.to().isPresent()) {
            predicates.add("f.start_date < :to");
        }
        return predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates);
    }

    private void bind(Query query, Filter filter) {
        filter.trainingCenterId().ifPresent(value -> query.setParameter("trainingCenterId", value));
        filter.programId().ifPresent(value -> query.setParameter("programId", value));
        filter.status().ifPresent(value -> query.setParameter("status", value));
        filter.from().ifPresent(value -> query.setParameter("from", value));
        filter.to().ifPresent(value -> query.setParameter("to", value));
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(Query query) {
        return (List<Object[]>) query.getResultList();
    }

    private QueryFichaReportsUseCase.EnrollmentByFichaItem toEnrollmentItem(Object[] row) {
        return new QueryFichaReportsUseCase.EnrollmentByFichaItem(
                uuid(row[0]),
                (String) row[1],
                uuid(row[2]),
                (String) row[3],
                (String) row[4],
                uuid(row[5]),
                (String) row[6],
                ((Number) row[7]).intValue(),
                (String) row[8],
                (String) row[9],
                localDate(row[10]));
    }

    private ProgressSource toProgressSource(Object[] row) {
        return new ProgressSource(
                uuid(row[0]),
                (String) row[1],
                (String) row[2],
                localDate(row[3]),
                nullableLocalDate(row[4]),
                nullableLocalDate(row[5]));
    }

    private String orderByEnrollment(Optional<String> sort) {
        return orderBy(sort, Map.of(
                "ficha_number", "f.ficha_number",
                "program_id", "f.program_id",
                "program_code", "p.program_code",
                "program_name", "p.name",
                "training_center_id", "f.training_center_id",
                "status", "f.status",
                "max_capacity", "f.max_capacity",
                "start_date", "f.start_date"), "f.start_date DESC, f.id DESC");
    }

    private String orderByProgress(Optional<String> sort) {
        return orderBy(sort, Map.of(
                "ficha_number", "f.ficha_number",
                "status", "f.status",
                "start_date", "f.start_date",
                "expected_end_date", "f.expected_end_date",
                "actual_end_date", "f.actual_end_date"), "f.start_date DESC, f.id DESC");
    }

    private String orderBy(Optional<String> requested,
                           Map<String, String> columns,
                           String defaultOrder) {
        if (requested.isEmpty() || requested.get().isBlank()) {
            return " ORDER BY " + defaultOrder;
        }
        String[] parts = requested.get().split(":", 2);
        String column = columns.get(parts[0]);
        if (column == null) {
            return " ORDER BY " + defaultOrder;
        }
        String direction = parts.length == 2 && "desc".equalsIgnoreCase(parts[1]) ? "DESC" : "ASC";
        return " ORDER BY " + column + " " + direction + ", f.id ASC";
    }

    private UUID uuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    private LocalDate localDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private LocalDate nullableLocalDate(Object value) {
        return value == null ? null : localDate(value);
    }
}
