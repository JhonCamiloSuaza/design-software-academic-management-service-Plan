package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.rest.security.JwtFeaturesConverter;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-ACAD-05 integration tests against real PostgreSQL. */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TrainingProgramDeactivationControllerIT {

        @SuppressWarnings("resource")
        @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("academic_management_training_program_deactivation_it")
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

    private final UUID actorId = UUID.randomUUID();

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

    private UUID seedKnowledgeNetwork() {
        UUID techLineId = UUID.randomUUID();
        UUID techNetworkId = UUID.randomUUID();
        UUID knowledgeNetworkId = UUID.randomUUID();

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
        return knowledgeNetworkId;
    }

    private String createProgram() throws Exception {
        UUID knowledgeNetworkId = seedKnowledgeNetwork();
        String programCode = "HU05-" + UUID.randomUUID().toString().substring(0, 8);
        return mockMvc.perform(post("/training-programs")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledge_network_id":"%s",
                                  "program_code":"%s",
                                  "name":"HU05 program",
                                  "training_level":"TECHNOLOGIST",
                                  "total_hours":2640
                                }
                                """.formatted(knowledgeNetworkId, programCode)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void e1_deletePerformsFullSoftDeleteAndReturns204() throws Exception {
        String created = createProgram();
        UUID programId = UUID.fromString(JsonPath.read(created, "$.id"));
        int rowVersion = JsonPath.read(created, "$.row_version");

        mockMvc.perform(delete("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row_version":%d}
                                """.formatted(rowVersion)))
                .andExpect(status().isNoContent());

        Boolean active = jdbcTemplate.queryForObject(
                "SELECT is_active FROM academic_management.training_program WHERE id = ?::uuid",
                Boolean.class,
                programId);
        Timestamp deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM academic_management.training_program WHERE id = ?::uuid",
                Timestamp.class,
                programId);
        UUID deletedBy = jdbcTemplate.queryForObject(
                "SELECT deleted_by FROM academic_management.training_program WHERE id = ?::uuid",
                UUID.class,
                programId);
        Integer newRowVersion = jdbcTemplate.queryForObject(
                "SELECT row_version FROM academic_management.training_program WHERE id = ?::uuid",
                Integer.class,
                programId);

        assertThat(active).isFalse();
        assertThat(deletedAt).isNotNull();
        assertThat(deletedBy).isEqualTo(actorId);
        assertThat(newRowVersion).isEqualTo(rowVersion + 1);
    }

    @Test
    void e1_rejectsStaleRowVersionWith409() throws Exception {
        String created = createProgram();
        String programId = JsonPath.read(created, "$.id");

        mockMvc.perform(put("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated before delete","row_version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.row_version").value(1));

        mockMvc.perform(delete("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row_version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROW_VERSION_MISMATCH"));
    }

    @Test
    void e1_requiresProgramManagePermission() throws Exception {
        String created = createProgram();
        String programId = JsonPath.read(created, "$.id");

        mockMvc.perform(delete("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row_version":0}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void e1_requiresRowVersionInDeleteBody() throws Exception {
        String created = createProgram();
        String programId = JsonPath.read(created, "$.id");

        mockMvc.perform(delete("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_VALIDATION_ERROR"));
    }

    @Test
    void e1_repeatDeleteIsIdempotentEvenWithStaleRowVersion() throws Exception {
        String created = createProgram();
        String programId = JsonPath.read(created, "$.id");

        mockMvc.perform(delete("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row_version":0}
                                """))
                .andExpect(status().isNoContent());

        // The resource is already soft-deleted. Repeating DELETE stays idempotent
        // even when the caller still has the previous row_version token.
        mockMvc.perform(delete("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row_version":0}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void e2_postFichaAgainstInactiveProgramReturns422() throws Exception {
        String created = createProgram();
        String programId = JsonPath.read(created, "$.id");

        mockMvc.perform(delete("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row_version":0}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/fichas")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ficha_number":"F-%s",
                                  "program_id":"%s",
                                  "training_center_id":"%s",
                                  "start_date":"%s",
                                  "training_shift":"DAY",
                                  "training_modality":"IN_PERSON",
                                  "max_capacity":30
                                }
                                """.formatted(
                                        UUID.randomUUID().toString().substring(0, 8),
                                        programId,
                                        UUID.randomUUID(),
                                        LocalDate.now().plusDays(10))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ACAD_TRAINING_PROGRAM_INACTIVE"));
    }

    @Test
    void e2_activeProgramCanOpenFichaWithFrozenProgramVersion() throws Exception {
        String created = createProgram();
        String programId = JsonPath.read(created, "$.id");
        String fichaNumber = "F-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/v1/fichas")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_FICHA_MANAGE:GLOBAL"))
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
                                """.formatted(fichaNumber, programId, UUID.randomUUID(),
                                        LocalDate.now().plusDays(10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ficha_number").value(fichaNumber))
                .andExpect(jsonPath("$.program_id").value(programId))
                .andExpect(jsonPath("$.program_version").value(1))
                .andExpect(jsonPath("$.status").value("INDUCTION"));
    }

    @Test
    void e3_existingFichaHistoryRemainsAfterProgramSoftDelete() throws Exception {
        String created = createProgram();
        UUID programId = UUID.fromString(JsonPath.read(created, "$.id"));
        UUID fichaId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO academic_management.enrollment_ficha "
                        + "(id, ficha_number, program_id, program_version, training_center_id, start_date, "
                        + "training_shift, training_modality, max_capacity, created_by, updated_by) "
                        + "VALUES (?::uuid, ?, ?::uuid, 1, ?::uuid, DATE '2026-09-01', "
                        + "'DAY', 'IN_PERSON', 30, ?::uuid, ?::uuid)",
                fichaId,
                "F-" + UUID.randomUUID().toString().substring(0, 8),
                programId,
                UUID.randomUUID(),
                actorId,
                actorId);

        mockMvc.perform(delete("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row_version":0}
                                """))
                .andExpect(status().isNoContent());

        Long fichaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_management.enrollment_ficha WHERE id = ?::uuid",
                Long.class,
                fichaId);
        Integer frozenVersion = jdbcTemplate.queryForObject(
                "SELECT program_version FROM academic_management.enrollment_ficha WHERE id = ?::uuid",
                Integer.class,
                fichaId);
        Long programCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_management.training_program WHERE id = ?::uuid",
                Long.class,
                programId);

        assertThat(fichaCount).isEqualTo(1L);
        assertThat(frozenVersion).isEqualTo(1);
        assertThat(programCount).isEqualTo(1L);
    }
}
