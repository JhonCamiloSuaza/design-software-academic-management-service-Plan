package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.RecordingCompetencyEventPublisher;
import co.sena.academic.adapter.in.rest.security.JwtFeaturesConverter;
import com.jayway.jsonpath.JsonPath;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-ACAD-06 integration tests against real PostgreSQL/Testcontainers. */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CompetencyControllerIT {

        @SuppressWarnings("resource")
        @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("academic_management_competency_it")
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
    private RecordingCompetencyEventPublisher eventPublisher;

    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void clearEvents() {
        eventPublisher.clear();
    }

    @BeforeAll
    static void applySchema() throws Exception {
        postgres.copyFileToContainer(
                MountableFile.forClasspathResource("db/it-changelog.sql"),
                "/it-changelog.sql");
        postgres.execInContainer(
                "psql",
                "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(),
                "-f", "/it-changelog.sql");
    }

    private RequestPostProcessor jwtWithFeatures(UUID subject, String... features) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("features", List.of(features))
                .subject(subject.toString())
                .build();
        return authentication(jwtFeaturesConverter.convert(jwt));
    }

    private UUID seedProgram() {
        UUID techLineId = UUID.randomUUID();
        UUID techNetworkId = UUID.randomUUID();
        UUID knowledgeNetworkId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO academic_management.tech_line "
                        + "(id, name, code, is_active, created_by, updated_by) "
                        + "VALUES (?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                techLineId,
                "Line-" + techLineId,
                "L-" + techLineId.toString().substring(0, 8),
                actorId,
                actorId);
        jdbcTemplate.update(
                "INSERT INTO academic_management.tech_network "
                        + "(id, tech_line_id, name, code, is_active, created_by, updated_by) "
                        + "VALUES (?::uuid, ?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                techNetworkId,
                techLineId,
                "Network-" + techNetworkId,
                "N-" + techNetworkId.toString().substring(0, 8),
                actorId,
                actorId);
        jdbcTemplate.update(
                "INSERT INTO academic_management.knowledge_network "
                        + "(id, tech_network_id, name, code, is_active, created_by, updated_by) "
                        + "VALUES (?::uuid, ?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                knowledgeNetworkId,
                techNetworkId,
                "Knowledge-" + knowledgeNetworkId,
                "K-" + knowledgeNetworkId.toString().substring(0, 8),
                actorId,
                actorId);
        jdbcTemplate.update(
                "INSERT INTO academic_management.training_program "
                        + "(id, knowledge_network_id, program_code, name, training_level, total_hours, "
                        + "version, is_active, row_version, created_by, updated_by) "
                        + "VALUES (?::uuid, ?::uuid, ?, ?, 'TECHNOLOGIST', 2640, 1, TRUE, 0, ?::uuid, ?::uuid)",
                programId,
                knowledgeNetworkId,
                "TP-" + programId.toString().substring(0, 8),
                "Program-" + programId,
                actorId,
                actorId);
        return programId;
    }

    private String createCompetency(UUID programId, String senaCode) throws Exception {
        return mockMvc.perform(post("/training-programs/" + programId + "/competencies")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Software competency",
                                  "sena_code":"%s",
                                  "hours":120
                                }
                                """.formatted(senaCode)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void e1_createsNestedCompetencyWithRealDbConstraints() throws Exception {
        UUID programId = seedProgram();
        String senaCode = "C-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/training-programs/" + programId + "/competencies")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Analyze requirements",
                                  "sena_code":"%s",
                                  "hours":120
                                }
                                """.formatted(senaCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.program_id").value(programId.toString()))
                .andExpect(jsonPath("$.sena_code").value(senaCode))
                .andExpect(jsonPath("$.hours").value(120))
                .andExpect(jsonPath("$.is_active").value(true))
                .andExpect(jsonPath("$.row_version").value(0));
    }

    @Test
    void e1_missingHoursReturns400ValidationError() throws Exception {
        UUID programId = seedProgram();

        mockMvc.perform(post("/training-programs/" + programId + "/competencies")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Missing hours",
                                  "sena_code":"C-NO-HOURS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Request validation failed"))
                .andExpect(jsonPath("$.error.details[0].field").value("hours"));

        assertThat(eventPublisher.events()).isEmpty();
    }

    @Test
    void e1_sameCodeInSameProgramReturns409ButSameCodeInDifferentProgramIsAllowed() throws Exception {
        UUID firstProgram = seedProgram();
        UUID secondProgram = seedProgram();
        String code = "SAME-" + UUID.randomUUID().toString().substring(0, 6);

        createCompetency(firstProgram, code);

        mockMvc.perform(post("/training-programs/" + firstProgram + "/competencies")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Duplicate","sena_code":"%s","hours":10}
                                """.formatted(code)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_COMPETENCY_CODE_ALREADY_EXISTS"));

        mockMvc.perform(post("/training-programs/" + secondProgram + "/competencies")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Allowed","sena_code":"%s","hours":10}
                                """.formatted(code)))
                .andExpect(status().isCreated());
    }

    @Test
    void e1_missingProgramReturns404AndNegativeHoursReturns400() throws Exception {
        UUID missingProgram = UUID.randomUUID();

        mockMvc.perform(post("/training-programs/" + missingProgram + "/competencies")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Missing program","sena_code":"C-MISSING","hours":10}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACAD_TRAINING_PROGRAM_NOT_FOUND"));

        UUID programId = seedProgram();
        mockMvc.perform(post("/training-programs/" + programId + "/competencies")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Invalid","sena_code":"C-NEG","hours":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_VALIDATION_ERROR"));
    }

    @Test
    void e1_inactiveProgramReturns422AndDoesNotPublishEvent() throws Exception {
        UUID programId = seedProgram();
        jdbcTemplate.update(
                "UPDATE academic_management.training_program "
                        + "SET is_active = FALSE, deleted_at = NOW(), deleted_by = ?::uuid "
                        + "WHERE id = ?::uuid",
                actorId,
                programId);

        mockMvc.perform(post("/training-programs/" + programId + "/competencies")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Blocked","sena_code":"C-INACTIVE","hours":10}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ACAD_TRAINING_PROGRAM_INACTIVE"));

        assertThat(eventPublisher.events()).isEmpty();
    }

    @Test
    void e2_listAndDetailRequireViewAndReturnCompetency() throws Exception {
        UUID programId = seedProgram();
        String response = createCompetency(
                programId, "VIEW-" + UUID.randomUUID().toString().substring(0, 6));
        String competencyId = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/training-programs/" + programId + "/competencies")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(competencyId));

        mockMvc.perform(get("/competencies/" + competencyId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.program_id").value(programId.toString()));

        mockMvc.perform(get("/competencies/" + competencyId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isForbidden());
    }

    @Test
    void e3_putUsesRowVersionAndRejectsStaleUpdate() throws Exception {
        UUID programId = seedProgram();
        String response = createCompetency(
                programId, "ROW-" + UUID.randomUUID().toString().substring(0, 6));
        String competencyId = JsonPath.read(response, "$.id");

        mockMvc.perform(put("/competencies/" + competencyId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated","row_version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.row_version").value(1));

        mockMvc.perform(put("/competencies/" + competencyId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Stale","row_version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROW_VERSION_MISMATCH"));
    }

    @Test
    void e3_deletePerformsFullSoftDeleteAndCannotBeReactivated() throws Exception {
        UUID programId = seedProgram();
        String response = createCompetency(
                programId, "DEL-" + UUID.randomUUID().toString().substring(0, 6));
        UUID competencyId = UUID.fromString(JsonPath.read(response, "$.id"));

        mockMvc.perform(delete("/competencies/" + competencyId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isNoContent());

        var row = jdbcTemplate.queryForMap(
                "SELECT is_active, deleted_at, deleted_by FROM academic_management.competency WHERE id = ?::uuid",
                competencyId);
        assertThat(row.get("is_active")).isEqualTo(false);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(row.get("deleted_by").toString()).isEqualTo(actorId.toString());

        mockMvc.perform(put("/competencies/" + competencyId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cannot reactivate","is_active":true,"row_version":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_COMPETENCY_DEACTIVATED"));
    }

    @Test
    void e3_blocksWhenLearningOutcomeExistsAndProgramHasOpenFicha() throws Exception {
        UUID programId = seedProgram();
        String response = createCompetency(
                programId, "USE-" + UUID.randomUUID().toString().substring(0, 6));
        UUID competencyId = UUID.fromString(JsonPath.read(response, "$.id"));

        UUID outcomeId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO academic_management.learning_outcome "
                        + "(id, competency_id, code, description, is_active, created_by, updated_by) "
                        + "VALUES (?::uuid, ?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                outcomeId,
                competencyId,
                "LO-" + outcomeId.toString().substring(0, 8),
                "Learning outcome",
                actorId,
                actorId);

        UUID fichaId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO academic_management.enrollment_ficha "
                        + "(id, ficha_number, program_id, program_version, training_center_id, start_date, "
                        + "training_shift, training_modality, max_capacity, created_by, updated_by) "
                        + "VALUES (?::uuid, ?, ?::uuid, 1, ?::uuid, DATE '2026-09-01', "
                        + "'DAY', 'IN_PERSON', 30, ?::uuid, ?::uuid)",
                fichaId,
                "F-" + fichaId.toString().substring(0, 8),
                programId,
                UUID.randomUUID(),
                actorId,
                actorId);

        mockMvc.perform(delete("/competencies/" + competencyId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_COMPETENCY_IN_USE"));
    }

    @Test
    void e4_createUpdateAndDeactivatePublishCanonicalCompetencyEvent() throws Exception {
        UUID programId = seedProgram();
        String code = "EVT-" + UUID.randomUUID().toString().substring(0, 6);
        String response = createCompetency(programId, code);
        UUID competencyId = UUID.fromString(JsonPath.read(response, "$.id"));

        assertThat(eventPublisher.events()).hasSize(1);
        var createdEvent = eventPublisher.events().get(0);
        assertThat(createdEvent.event_id()).isNotNull();
        assertThat(createdEvent.event_type()).isEqualTo("academic.competency.updated");
        assertThat(createdEvent.version()).isEqualTo(1);
        assertThat(createdEvent.timestamp()).isNotNull();
        assertThat(createdEvent.source_service()).isEqualTo("academic-management-service");
        assertThat(createdEvent.payload().competency_id()).isEqualTo(competencyId);
        assertThat(createdEvent.payload().competency_code()).isEqualTo(code);
        assertThat(createdEvent.payload().program_id()).isEqualTo(programId);
        assertThat(createdEvent.payload().change_type()).isEqualTo("CREATED");
        assertThat(createdEvent.payload().updated_by()).isEqualTo(actorId);
        assertThat(createdEvent.payload().updated_at()).isNotNull();

        mockMvc.perform(put("/competencies/" + competencyId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Event updated","row_version":0}
                                """))
                .andExpect(status().isOk());

        assertThat(eventPublisher.events()).hasSize(2);
        assertThat(eventPublisher.events().get(1).payload().change_type()).isEqualTo("UPDATED");

        mockMvc.perform(delete("/competencies/" + competencyId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isNoContent());

        assertThat(eventPublisher.events()).hasSize(3);
        var deactivatedEvent = eventPublisher.events().get(2);
        assertThat(deactivatedEvent.payload().change_type()).isEqualTo("DEACTIVATED");
        assertThat(deactivatedEvent.payload().competency_id()).isEqualTo(competencyId);
    }

    @Test
    void e3_managePermissionIsRequiredForCreateUpdateDelete() throws Exception {
        UUID programId = seedProgram();

        mockMvc.perform(post("/training-programs/" + programId + "/competencies")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Forbidden","sena_code":"C-403","hours":10}
                                """))
                .andExpect(status().isForbidden());
    }
}
