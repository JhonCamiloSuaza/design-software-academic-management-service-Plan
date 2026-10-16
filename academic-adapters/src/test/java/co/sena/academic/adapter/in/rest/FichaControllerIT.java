package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.RecordingFichaEventPublisher;
import co.sena.academic.adapter.RecordingFichaOwnershipPort;
import co.sena.academic.adapter.RecordingTrainingCenterLookup;
import co.sena.academic.adapter.in.rest.security.JwtFeaturesConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class FichaControllerIT {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("academic_management_ficha_it")
            .withUsername("it_user")
            .withPassword("it_pass");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "https://example.invalid/jwks.json");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtFeaturesConverter jwtFeaturesConverter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingFichaEventPublisher eventPublisher;

    @Autowired
    private RecordingTrainingCenterLookup trainingCenterLookup;

    @Autowired
    private RecordingFichaOwnershipPort fichaOwnershipPort;

    private final UUID actorId = UUID.randomUUID();

    @BeforeAll
    static void applySchema() throws Exception {
        postgres.copyFileToContainer(
                MountableFile.forClasspathResource("db/it-changelog.sql"),
                "/it-changelog.sql");
        postgres.execInContainer(
                "psql", "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(), "-f", "/it-changelog.sql");
    }

    @BeforeEach
    void clearTestDoubles() {
        eventPublisher.clear();
        trainingCenterLookup.clear();
        fichaOwnershipPort.clear();
    }

    @Test
    void e1_createsFichaWithFrozenProgramVersionAndPublishesOpenedEvent() throws Exception {
        UUID programId = seedProgram(4);
        UUID centerId = UUID.randomUUID();
        String fichaNumber = "F-" + UUID.randomUUID().toString().substring(0, 8);
        String startDate = LocalDate.now().plusDays(10).toString();

        mockMvc.perform(post("/api/v1/fichas")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_FICHA_MANAGE:TRAINING_CENTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ficha_number":"%s",
                                  "program_id":"%s",
                                  "training_center_id":"%s",
                                  "start_date":"%s",
                                  "training_shift":"DAY",
                                  "training_modality":"IN_PERSON",
                                  "max_capacity":30
                                }
                                """.formatted(fichaNumber, programId, centerId, startDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ficha_number").value(fichaNumber))
                .andExpect(jsonPath("$.program_version").value(4))
                .andExpect(jsonPath("$.status").value("INDUCTION"));

        Integer snapshot = jdbcTemplate.queryForObject(
                "SELECT program_version FROM academic_management.enrollment_ficha WHERE ficha_number = ?",
                Integer.class, fichaNumber);
        org.assertj.core.api.Assertions.assertThat(snapshot).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(eventPublisher.events()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(eventPublisher.events().get(0).event_type())
                .isEqualTo("academic.ficha.opened");
    }

    @Test
    void e1_rejectsDuplicateFichaNumberWith409() throws Exception {
        UUID programId = seedProgram(1);
        UUID centerId = UUID.randomUUID();
        String fichaNumber = "F-DUP-" + UUID.randomUUID().toString().substring(0, 8);
        String startDate = LocalDate.now().plusDays(10).toString();
        String payload = """
                {
                  "ficha_number":"%s",
                  "program_id":"%s",
                  "training_center_id":"%s",
                  "start_date":"%s",
                  "training_shift":"DAY",
                  "training_modality":"IN_PERSON",
                  "max_capacity":30
                }
                """.formatted(fichaNumber, programId, centerId, startDate);

        mockMvc.perform(post("/api/v1/fichas")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_FICHA_MANAGE:TRAINING_CENTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/fichas")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_FICHA_MANAGE:TRAINING_CENTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_FICHA_NUMBER_ALREADY_EXISTS"));
    }

    @Test
    void e2_rejectsNonExistingProgramWith422() throws Exception {
        mockMvc.perform(post("/api/v1/fichas")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_FICHA_MANAGE:TRAINING_CENTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ficha_number":"F-NOT-FOUND",
                                  "program_id":"%s",
                                  "training_center_id":"%s",
                                  "start_date":"%s",
                                  "training_shift":"DAY",
                                  "training_modality":"IN_PERSON",
                                  "max_capacity":30
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(),
                                LocalDate.now().plusDays(10))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ACAD_TRAINING_PROGRAM_NOT_FOUND"));
    }

    @Test
    void e2_rejectsStartDateInThePast() throws Exception {
        mockMvc.perform(post("/api/v1/fichas")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_FICHA_MANAGE:TRAINING_CENTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ficha_number":"F-PAST",
                                  "program_id":"%s",
                                  "training_center_id":"%s",
                                  "start_date":"%s",
                                  "training_shift":"DAY",
                                  "training_modality":"IN_PERSON",
                                  "max_capacity":30
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(),
                                LocalDate.now().minusDays(1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ACAD_FICHA_START_DATE_NOT_FUTURE"));
    }

    @Test
    void e3_rejectsInactiveTrainingCenterFromExplicitDouble() throws Exception {
        UUID programId = seedProgram(1);
        UUID centerId = UUID.randomUUID();
        trainingCenterLookup.markInactive(centerId);

        mockMvc.perform(post("/api/v1/fichas")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_FICHA_MANAGE:TRAINING_CENTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ficha_number":"F-INACTIVE",
                                  "program_id":"%s",
                                  "training_center_id":"%s",
                                  "start_date":"%s",
                                  "training_shift":"NIGHT",
                                  "training_modality":"HYBRID",
                                  "max_capacity":15
                                }
                                """.formatted(programId, centerId, LocalDate.now().plusDays(3))))
                .andExpect(status().isUnprocessableEntity())
                 .andExpect(jsonPath("$.error.code").value("ACAD_TRAINING_CENTER_INACTIVE"));
    }

    @Test
    void hu09_contractRoutesAreAvailableUnderApiV1() throws Exception {
        UUID programId = seedProgram(12);
        UUID fichaId = insertFicha(programId, UUID.randomUUID(), "F-09-API-V1", "EXECUTION",
                LocalDate.of(2026, 12, 1));

        mockMvc.perform(get("/api/v1/fichas")
                        .param("ficha_number", "F-09-API-V1")
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fichaId.toString()));
    }

    @Test
    void hu09_globalListsWithFiltersPaginationAndExclusiveToDate() throws Exception {
        UUID programA = seedProgram(1);
        UUID programB = seedProgram(2);
        UUID centerA = UUID.randomUUID();
        UUID centerB = UUID.randomUUID();
        UUID first = insertFicha(programA, centerA, "F-09-A1", "INDUCTION", LocalDate.of(2026, 1, 1));
        insertFicha(programA, centerA, "F-09-A2", "EXECUTION", LocalDate.of(2026, 2, 1));
        insertFicha(programB, centerB, "F-09-B1", "EXECUTION", LocalDate.of(2026, 3, 1));

        mockMvc.perform(get("/api/v1/fichas")
                        .param("program_id", programA.toString())
                        .param("from", "2026-01-01")
                        .param("to", "2026-02-01")
                        .param("page", "1")
                        .param("page_size", "1")
                        .param("sort", "start_date:asc")
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(first.toString()))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.page_size").value(1))
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.pagination.total_pages").value(1));
    }

    @Test
    void hu09_appliesAllSupportedFiltersWithExactFichaNumber() throws Exception {
        UUID program = seedProgram(3);
        UUID otherProgram = seedProgram(4);
        UUID center = UUID.randomUUID();
        UUID otherCenter = UUID.randomUUID();
        UUID expected = insertFicha(program, center, "F-09-EXACT", "EXECUTION",
                LocalDate.of(2026, 7, 10));
        insertFicha(program, center, "F-09-EXACT-SUFFIX", "EXECUTION",
                LocalDate.of(2026, 7, 10));
        insertFicha(program, center, "F-09-OTHER-STATUS", "COMPLETED",
                LocalDate.of(2026, 7, 10));
        insertFicha(otherProgram, otherCenter, "F-09-OTHER-PROGRAM", "EXECUTION",
                LocalDate.of(2026, 7, 10));

        mockMvc.perform(get("/api/v1/fichas")
                        .param("program_id", program.toString())
                        .param("status", "EXECUTION")
                        .param("ficha_number", "F-09-EXACT")
                        .param("training_center_id", center.toString())
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-11")
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(expected.toString()))
                .andExpect(jsonPath("$.pagination.total_items").value(1));
    }

    @Test
    void hu09_paginationReturnsDistinctPages() throws Exception {
        UUID program = seedProgram(5);
        UUID center = UUID.randomUUID();
        UUID first = insertFicha(program, center, "F-09-PAGE-1", "EXECUTION",
                LocalDate.of(2026, 8, 1));
        UUID second = insertFicha(program, center, "F-09-PAGE-2", "EXECUTION",
                LocalDate.of(2026, 8, 2));

        mockMvc.perform(get("/api/v1/fichas")
                        .param("program_id", program.toString())
                        .param("page", "1")
                        .param("page_size", "1")
                        .param("sort", "start_date:asc")
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(first.toString()))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.total_items").value(2))
                .andExpect(jsonPath("$.pagination.total_pages").value(2));

        mockMvc.perform(get("/api/v1/fichas")
                        .param("program_id", program.toString())
                        .param("page", "2")
                        .param("page_size", "1")
                        .param("sort", "start_date:asc")
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(second.toString()))
                .andExpect(jsonPath("$.pagination.page").value(2));
    }

    @Test
    void hu09_rejectsInvalidStatusAndPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/fichas")
                        .param("status", "NOT_A_STATUS")
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/fichas")
                        .param("page_size", "101")
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hu09_trainingCenterScopeIsForcedEvenWhenQueryTriesAnotherCenter() throws Exception {
        UUID programId = seedProgram(1);
        UUID ownCenter = UUID.randomUUID();
        UUID otherCenter = UUID.randomUUID();
        UUID ownFicha = insertFicha(programId, ownCenter, "F-09-CENTER-A", "EXECUTION",
                LocalDate.of(2026, 4, 1));
        UUID otherFicha = insertFicha(programId, otherCenter, "F-09-CENTER-B", "EXECUTION",
                LocalDate.of(2026, 5, 1));

        mockMvc.perform(get("/api/v1/fichas")
                        .with(jwtWithClaims(actorId, null, ownCenter,
                                "ACADEMIC_FICHA_VIEW:TRAINING_CENTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.data[0].id").value(ownFicha.toString()));

        mockMvc.perform(get("/api/v1/fichas")
                        .param("training_center_id", otherCenter.toString())
                        .with(jwtWithClaims(actorId, null, ownCenter,
                                "ACADEMIC_FICHA_VIEW:TRAINING_CENTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.pagination.total_items").value(0));

        mockMvc.perform(get("/api/v1/fichas/{id}", otherFicha)
                        .with(jwtWithClaims(actorId, null, ownCenter,
                                "ACADEMIC_FICHA_VIEW:TRAINING_CENTER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACAD_FICHA_NOT_FOUND"));
    }

    @Test
    void hu09_detailReturnsFrozenProgramVersionAndAuditTimestamps() throws Exception {
        UUID programId = seedProgram(7);
        UUID centerId = UUID.randomUUID();
        UUID fichaId = insertFicha(programId, 7, centerId, "F-09-DETAIL", "PRODUCTIVE_STAGE",
                LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fichaId.toString()))
                .andExpect(jsonPath("$.program_version").value(7))
                .andExpect(jsonPath("$.status").value("PRODUCTIVE_STAGE"))
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andExpect(jsonPath("$.updated_at").isNotEmpty());
    }

    @Test
    void hu10_updatesOperationalFieldsAndAdvancesRowVersion() throws Exception {
        UUID programId = seedProgram(12);
        UUID centerId = UUID.randomUUID();
        UUID fichaId = insertFicha(programId, centerId, "F-10-UPDATE", "EXECUTION",
                LocalDate.of(2026, 12, 1));

        mockMvc.perform(put("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "training_shift":"NIGHT",
                                  "training_modality":"HYBRID",
                                  "expected_end_date":"2028-12-01",
                                  "max_capacity":20,
                                  "row_version":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.training_shift").value("NIGHT"))
                .andExpect(jsonPath("$.training_modality").value("HYBRID"))
                .andExpect(jsonPath("$.max_capacity").value(20))
                .andExpect(jsonPath("$.row_version").value(1));

        Integer persistedCapacity = jdbcTemplate.queryForObject(
                "SELECT max_capacity FROM academic_management.enrollment_ficha WHERE id = ?::uuid",
                Integer.class, fichaId);
        org.assertj.core.api.Assertions.assertThat(persistedCapacity).isEqualTo(20);
    }

    @Test
    void hu10_rejectsCapacityIncrease() throws Exception {
        UUID programId = seedProgram(13);
        UUID fichaId = insertFicha(programId, UUID.randomUUID(), "F-10-INCREASE", "EXECUTION",
                LocalDate.of(2026, 12, 2));

        mockMvc.perform(put("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"max_capacity\":31,\"row_version\":0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code")
                        .value("ACAD_FICHA_CAPACITY_INCREASE_NOT_ALLOWED"));
    }

    @Test
    void hu10_rejectsCapacityBelowActiveLearners() throws Exception {
        UUID programId = seedProgram(17);
        UUID fichaId = insertFicha(programId, UUID.randomUUID(), "F-10-ACTIVE-LEARNERS", "EXECUTION",
                LocalDate.of(2026, 12, 6));
        fichaOwnershipPort.activeLearners(fichaId, 3);

        mockMvc.perform(put("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"max_capacity\":2,\"row_version\":0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code")
                        .value("ACAD_FICHA_CAPACITY_BELOW_ACTIVE_LEARNERS"));
    }

    @Test
    void hu10_rejectsTerminalStatus() throws Exception {
        UUID programId = seedProgram(14);
        UUID fichaId = insertFicha(programId, UUID.randomUUID(), "F-10-TERMINAL", "COMPLETED",
                LocalDate.of(2026, 12, 3));

        mockMvc.perform(put("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"training_shift\":\"NIGHT\",\"row_version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_TERMINAL_STATUS"));
    }

    @Test
    void hu10_rejectsStaleRowVersion() throws Exception {
        UUID programId = seedProgram(15);
        UUID fichaId = insertFicha(programId, UUID.randomUUID(), "F-10-STALE", "EXECUTION",
                LocalDate.of(2026, 12, 4));
        String body = "{\"training_shift\":\"NIGHT\",\"row_version\":0}";

        mockMvc.perform(put("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROW_VERSION_MISMATCH"));
    }

    @Test
    void hu10_rejectsImmutableFieldsAndStatusInRequest() throws Exception {
        UUID programId = seedProgram(16);
        UUID fichaId = insertFicha(programId, UUID.randomUUID(), "F-10-IMMUTABLE", "EXECUTION",
                LocalDate.of(2026, 12, 5));

        mockMvc.perform(put("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ficha_number\":\"OTHER\",\"row_version\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"program_id\":\"00000000-0000-0000-0000-000000000001\",\"row_version\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"program_version\":99,\"row_version\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaims(actorId, null, null,
                                "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\",\"row_version\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hu11_patchStatusCancelsFichaAndPublishesClosedEnvelope() throws Exception {
        UUID programId = seedProgram(18);
        UUID centerId = UUID.randomUUID();
        UUID fichaId = insertFicha(programId, centerId, "F-11-CANCEL", "EXECUTION",
                LocalDate.of(2026, 12, 7));

        mockMvc.perform(patch("/api/v1/fichas/{id}/status", fichaId)
                        .with(jwtWithClaimsAndRoles(actorId, null, centerId,
                                List.of("CENTER_DIRECTOR"),
                                "ACADEMIC_FICHA_STATUS:TRAINING_CENTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"CANCELLED",
                                  "actual_end_date":"2026-12-20"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.actual_end_date").value("2026-12-20"));

        String persistedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM academic_management.enrollment_ficha WHERE id = ?::uuid",
                String.class, fichaId);
        org.assertj.core.api.Assertions.assertThat(persistedStatus).isEqualTo("CANCELLED");

        org.assertj.core.api.Assertions.assertThat(eventPublisher.events()).hasSize(1);
        var event = eventPublisher.events().get(0);
        org.assertj.core.api.Assertions.assertThat(event.event_id()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(event.event_type()).isEqualTo("academic.ficha.closed");
        org.assertj.core.api.Assertions.assertThat(event.version()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(event.timestamp()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(event.source_service()).isEqualTo("academic-management-service");
        org.assertj.core.api.Assertions.assertThat(event.payload().ficha_id()).isEqualTo(fichaId);
        org.assertj.core.api.Assertions.assertThat(event.payload().close_reason()).isEqualTo("CANCELLED");
        org.assertj.core.api.Assertions.assertThat(event.payload().actual_end_date())
                .isEqualTo(LocalDate.of(2026, 12, 20));
    }

    @Test
    void hu11_rejectsTerminalStatusWith409AndDoesNotPublishEvent() throws Exception {
        UUID programId = seedProgram(19);
        UUID fichaId = insertFicha(programId, UUID.randomUUID(), "F-11-TERMINAL", "COMPLETED",
                LocalDate.of(2026, 12, 8));

        mockMvc.perform(patch("/api/v1/fichas/{id}/status", fichaId)
                        .with(jwtWithClaimsAndRoles(actorId, null, null,
                                List.of("CENTER_DIRECTOR"),
                                "ACADEMIC_FICHA_STATUS:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_TERMINAL_STATUS"));

        org.assertj.core.api.Assertions.assertThat(eventPublisher.events()).isEmpty();
    }

    @Test
    void hu11_rejectsCancellationWithoutCenterDirectorRole() throws Exception {
        UUID programId = seedProgram(20);
        UUID fichaId = insertFicha(programId, UUID.randomUUID(), "F-11-ROLE", "EXECUTION",
                LocalDate.of(2026, 12, 9));

        mockMvc.perform(patch("/api/v1/fichas/{id}/status", fichaId)
                        .with(jwtWithClaimsAndRoles(actorId, null, null,
                                List.of("COORDINATOR"),
                                "ACADEMIC_FICHA_STATUS:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACAD_FICHA_STATUS_ROLE_DENIED"));

        org.assertj.core.api.Assertions.assertThat(eventPublisher.events()).isEmpty();
    }

    @Test
    void hu12_deleteReusesCancellationUseCaseAndPublishesClosedEvent() throws Exception {
        UUID programId = seedProgram(21);
        UUID centerId = UUID.randomUUID();
        UUID fichaId = insertFicha(programId, centerId, "F-12-DELETE", "PRODUCTIVE_STAGE",
                LocalDate.of(2026, 12, 10));

        mockMvc.perform(delete("/api/v1/fichas/{id}", fichaId)
                        .with(jwtWithClaimsAndRoles(actorId, null, null,
                                List.of("CENTER_DIRECTOR"),
                                "ACADEMIC_FICHA_STATUS:GLOBAL")))
                .andExpect(status().isNoContent());

        String persistedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM academic_management.enrollment_ficha WHERE id = ?::uuid",
                String.class, fichaId);
        org.assertj.core.api.Assertions.assertThat(persistedStatus).isEqualTo("CANCELLED");
        org.assertj.core.api.Assertions.assertThat(eventPublisher.events()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(eventPublisher.events().get(0).event_type())
                .isEqualTo("academic.ficha.closed");
    }

    @Test
    void hu13_failedBusinessValidationDoesNotPublishEvent() throws Exception {
        mockMvc.perform(post("/api/v1/fichas")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_FICHA_MANAGE:TRAINING_CENTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ficha_number":"F-13-422",
                                  "program_id":"%s",
                                  "training_center_id":"%s",
                                  "start_date":"%s",
                                  "training_shift":"DAY",
                                  "training_modality":"IN_PERSON",
                                  "max_capacity":30
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(),
                                LocalDate.now().plusDays(10))))
                .andExpect(status().isUnprocessableEntity());

        org.assertj.core.api.Assertions.assertThat(eventPublisher.events()).isEmpty();
    }

    @Test
    void hu09_fourScopesReturnDifferentSets() throws Exception {
        UUID programId = seedProgram(9);
        UUID centerA = UUID.randomUUID();
        UUID centerB = UUID.randomUUID();
        UUID globalA = insertFicha(programId, centerA, "F-09-GLOBAL", "EXECUTION",
                LocalDate.of(2026, 9, 1));
        UUID instructorFicha = insertFicha(programId, centerA, "F-09-INSTRUCTOR", "EXECUTION",
                LocalDate.of(2026, 9, 2));
        UUID learnerFicha = insertFicha(programId, centerB, "F-09-LEARNER", "EXECUTION",
                LocalDate.of(2026, 9, 3));

        UUID instructorId = UUID.randomUUID();
        UUID learnerId = UUID.randomUUID();
        fichaOwnershipPort.instructorOwns(instructorId, instructorFicha);
        fichaOwnershipPort.learnerOwns(learnerId, learnerFicha);

        mockMvc.perform(get("/api/v1/fichas").param("program_id", programId.toString())
                        .with(jwtWithClaims(actorId, null, null, "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(3));

        mockMvc.perform(get("/api/v1/fichas").param("program_id", programId.toString())
                        .with(jwtWithClaims(actorId, null, centerA,
                                "ACADEMIC_FICHA_VIEW:TRAINING_CENTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(2));

        mockMvc.perform(get("/api/v1/fichas").param("program_id", programId.toString())
                        .with(jwtWithClaims(actorId, instructorId, null,
                                "ACADEMIC_FICHA_VIEW_OWN:OWN_FICHAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.data[0].id").value(instructorFicha.toString()));

        mockMvc.perform(get("/api/v1/fichas").param("program_id", programId.toString())
                        .with(jwtWithClaims(actorId, learnerId, null,
                                "ACADEMIC_FICHA_VIEW_OWN:OWN_FICHA_AS_LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.data[0].id").value(learnerFicha.toString()));

        org.assertj.core.api.Assertions.assertThat(globalA)
                .isNotEqualTo(instructorFicha)
                .isNotEqualTo(learnerFicha);
    }

    @Test
    void hu09_ownDetailReturnsOnlyOwnedFichaAndHidesOtherFicha() throws Exception {
        UUID programId = seedProgram(10);
        UUID centerId = UUID.randomUUID();
        UUID allowed = insertFicha(programId, centerId, "F-09-OWN-DETAIL", "EXECUTION",
                LocalDate.of(2026, 10, 1));
        UUID hidden = insertFicha(programId, centerId, "F-09-HIDDEN-DETAIL", "EXECUTION",
                LocalDate.of(2026, 10, 2));
        UUID instructorId = UUID.randomUUID();
        fichaOwnershipPort.instructorOwns(instructorId, allowed);

        mockMvc.perform(get("/api/v1/fichas")
                        .with(jwtWithClaims(actorId, instructorId, null,
                                "ACADEMIC_FICHA_VIEW_OWN:OWN_FICHAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(1));

        mockMvc.perform(get("/api/v1/fichas/{id}", allowed)
                        .with(jwtWithClaims(actorId, instructorId, null,
                                "ACADEMIC_FICHA_VIEW_OWN:OWN_FICHAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(allowed.toString()));

        mockMvc.perform(get("/api/v1/fichas/{id}", hidden)
                        .with(jwtWithClaims(actorId, instructorId, null,
                                "ACADEMIC_FICHA_VIEW_OWN:OWN_FICHAS")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACAD_FICHA_NOT_FOUND"));
    }

    @Test
    void hu09_learnerOwnScopeUsesLearnerOwnership() throws Exception {
        UUID programId = seedProgram(11);
        UUID centerId = UUID.randomUUID();
        UUID allowed = insertFicha(programId, centerId, "F-09-LEARNER-OWN", "EXECUTION",
                LocalDate.of(2026, 11, 1));
        UUID hidden = insertFicha(programId, centerId, "F-09-LEARNER-HIDDEN", "EXECUTION",
                LocalDate.of(2026, 11, 2));
        UUID learnerId = UUID.randomUUID();
        fichaOwnershipPort.learnerOwns(learnerId, allowed);

        mockMvc.perform(get("/api/v1/fichas")
                        .param("program_id", programId.toString())
                        .with(jwtWithClaims(actorId, learnerId, null,
                                "ACADEMIC_FICHA_VIEW_OWN:OWN_FICHA_AS_LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.data[0].id").value(allowed.toString()));

        mockMvc.perform(get("/api/v1/fichas/{id}", hidden)
                        .with(jwtWithClaims(actorId, learnerId, null,
                                "ACADEMIC_FICHA_VIEW_OWN:OWN_FICHA_AS_LEARNER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void hu09_detailWithUnknownUuidReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/fichas/{id}", UUID.randomUUID())
                        .with(jwtWithClaims(actorId, null, null, "ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACAD_FICHA_NOT_FOUND"));
    }

    @Test
    void hu09_requiresFichaViewFeature() throws Exception {
        mockMvc.perform(get("/api/v1/fichas"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/fichas")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor jwtWithFeatures(UUID subject, String... features) {
        return jwtWithClaims(subject, null, null, features);
    }

    private RequestPostProcessor jwtWithClaims(UUID subject,
                                               UUID actorClaim,
                                               UUID trainingCenterClaim,
                                               String... features) {
        return jwtWithClaimsAndRoles(subject, actorClaim, trainingCenterClaim, List.of(), features);
    }

    private RequestPostProcessor jwtWithClaimsAndRoles(UUID subject,
                                                       UUID actorClaim,
                                                       UUID trainingCenterClaim,
                                                       List<String> roles,
                                                       String... features) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("features", List.of(features))
                .claim("roles", roles)
                .subject(subject.toString());
        if (actorClaim != null) {
            builder.claim("actor_id", actorClaim.toString());
        }
        if (trainingCenterClaim != null) {
            builder.claim("training_center_id", trainingCenterClaim.toString());
        }
        return authentication(jwtFeaturesConverter.convert(builder.build()));
    }

    private UUID insertFicha(UUID programId,
                             UUID trainingCenterId,
                             String fichaNumber,
                             String status,
                             LocalDate startDate) {
        return insertFicha(programId, 1, trainingCenterId, fichaNumber, status, startDate);
    }

    private UUID insertFicha(UUID programId,
                             int programVersion,
                             UUID trainingCenterId,
                             String fichaNumber,
                             String status,
                             LocalDate startDate) {
        UUID fichaId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO academic_management.enrollment_ficha "
                        + "(id, ficha_number, program_id, program_version, training_center_id, status, "
                        + "start_date, expected_end_date, training_shift, training_modality, max_capacity, "
                        + "row_version, created_by, updated_by) "
                        + "VALUES (?::uuid, ?, ?::uuid, ?, ?::uuid, ?, ?, ?, 'DAY', 'IN_PERSON', 30, 0, ?::uuid, ?::uuid)",
                fichaId, fichaNumber, programId, programVersion, trainingCenterId, status,
                startDate, startDate.plusYears(2), actorId, actorId);
        return fichaId;
    }

    private UUID seedProgram(int version) {
        UUID techLineId = UUID.randomUUID();
        UUID techNetworkId = UUID.randomUUID();
        UUID knowledgeNetworkId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO academic_management.tech_line "
                        + "(id, name, code, is_active, created_by, updated_by) VALUES (?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                techLineId, "Line-" + techLineId, "L-" + techLineId.toString().substring(0, 8), actorId, actorId);
        jdbcTemplate.update(
                "INSERT INTO academic_management.tech_network "
                        + "(id, tech_line_id, name, code, is_active, created_by, updated_by) VALUES (?::uuid, ?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                techNetworkId, techLineId, "Network-" + techNetworkId,
                "N-" + techNetworkId.toString().substring(0, 8), actorId, actorId);
        jdbcTemplate.update(
                "INSERT INTO academic_management.knowledge_network "
                        + "(id, tech_network_id, name, code, is_active, created_by, updated_by) VALUES (?::uuid, ?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                knowledgeNetworkId, techNetworkId, "Knowledge-" + knowledgeNetworkId,
                "K-" + knowledgeNetworkId.toString().substring(0, 8), actorId, actorId);
        jdbcTemplate.update(
                "INSERT INTO academic_management.training_program "
                        + "(id, knowledge_network_id, program_code, name, training_level, total_hours, version, is_active, row_version, created_by, updated_by) "
                        + "VALUES (?::uuid, ?::uuid, ?, ?, 'TECHNOLOGIST', 2640, ?, TRUE, 0, ?::uuid, ?::uuid)",
                programId, knowledgeNetworkId, "P-" + programId.toString().substring(0, 8),
                "Program-" + programId, version, actorId, actorId);
        return programId;
    }
}
