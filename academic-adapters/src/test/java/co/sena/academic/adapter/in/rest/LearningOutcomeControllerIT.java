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

/** HU-ACAD-07 integration tests against real PostgreSQL/Testcontainers. */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class LearningOutcomeControllerIT {

        @SuppressWarnings("resource")
        @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("academic_management_learning_outcome_it")
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
                "psql", "-U", postgres.getUsername(), "-d", postgres.getDatabaseName(), "-f", "/it-changelog.sql");
    }

    private RequestPostProcessor jwtWithFeatures(UUID subject, String... features) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("features", List.of(features))
                .subject(subject.toString())
                .build();
        return authentication(jwtFeaturesConverter.convert(jwt));
    }

    private UUID seedCompetency() {
        UUID techLineId = UUID.randomUUID();
        UUID techNetworkId = UUID.randomUUID();
        UUID knowledgeNetworkId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID competencyId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO academic_management.tech_line "
                        + "(id, name, code, is_active, created_by, updated_by) VALUES (?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                techLineId, "Line-" + techLineId, "L-" + techLineId.toString().substring(0, 8), actorId, actorId);
        jdbcTemplate.update("INSERT INTO academic_management.tech_network "
                        + "(id, tech_line_id, name, code, is_active, created_by, updated_by) VALUES (?::uuid, ?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                techNetworkId, techLineId, "Network-" + techNetworkId, "N-" + techNetworkId.toString().substring(0, 8), actorId, actorId);
        jdbcTemplate.update("INSERT INTO academic_management.knowledge_network "
                        + "(id, tech_network_id, name, code, is_active, created_by, updated_by) VALUES (?::uuid, ?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                knowledgeNetworkId, techNetworkId, "Knowledge-" + knowledgeNetworkId, "K-" + knowledgeNetworkId.toString().substring(0, 8), actorId, actorId);
        jdbcTemplate.update("INSERT INTO academic_management.training_program "
                        + "(id, knowledge_network_id, program_code, name, training_level, total_hours, version, is_active, row_version, created_by, updated_by) "
                        + "VALUES (?::uuid, ?::uuid, ?, ?, 'TECHNOLOGIST', 2640, 1, TRUE, 0, ?::uuid, ?::uuid)",
                programId, knowledgeNetworkId, "TP-" + programId.toString().substring(0, 8), "Program-" + programId, actorId, actorId);
        jdbcTemplate.update("INSERT INTO academic_management.competency "
                        + "(id, program_id, name, sena_code, hours, is_active, row_version, created_by, updated_by) "
                        + "VALUES (?::uuid, ?::uuid, ?, ?, 120, TRUE, 0, ?::uuid, ?::uuid)",
                competencyId, programId, "Competency-" + competencyId, "C-" + competencyId.toString().substring(0, 8), actorId, actorId);
        return competencyId;
    }

    private String createOutcome(UUID competencyId, String code) throws Exception {
        return mockMvc.perform(post("/api/v1/competencies/" + competencyId + "/learning-outcomes")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","description":"Describe a solution","is_active":true}
                                """.formatted(code)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void e1_createsOutcomeAndRejectsDuplicateCodeWithinCompetency() throws Exception {
        UUID competencyId = seedCompetency();
        String code = "LO-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/v1/competencies/" + competencyId + "/learning-outcomes")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"description\":\"Describe a solution\"}".formatted(code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.competency_id").value(competencyId.toString()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.row_version").value(0))
                .andExpect(jsonPath("$.is_active").value(true));

        mockMvc.perform(post("/api/v1/competencies/" + competencyId + "/learning-outcomes")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"description\":\"Duplicate\"}".formatted(code)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_LEARNING_OUTCOME_CODE_ALREADY_EXISTS"));
    }

    @Test
    void e2_listsNestedOutcomesOnlyWithViewPermission() throws Exception {
        UUID competencyId = seedCompetency();
        createOutcome(competencyId, "LO-" + UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(get("/api/v1/competencies/" + competencyId + "/learning-outcomes")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination.total_items").value(1));

        mockMvc.perform(get("/api/v1/competencies/" + competencyId + "/learning-outcomes")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/learning-outcomes")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isNotFound());
    }

    @Test
    void e3_updatesWithRowVersionAndDeactivatesWithoutDeletingTheRow() throws Exception {
        UUID competencyId = seedCompetency();
        String response = createOutcome(competencyId, "LO-" + UUID.randomUUID().toString().substring(0, 8));
        UUID outcomeId = UUID.fromString(JsonPath.read(response, "$.id"));
        int rowVersion = JsonPath.read(response, "$.row_version");

        mockMvc.perform(put("/api/v1/learning-outcomes/" + outcomeId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Updated\",\"row_version\":%d}".formatted(rowVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"))
                .andExpect(jsonPath("$.row_version").value(rowVersion + 1));

        mockMvc.perform(put("/api/v1/learning-outcomes/" + outcomeId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Stale\",\"row_version\":%d}".formatted(rowVersion)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROW_VERSION_MISMATCH"));

        mockMvc.perform(delete("/api/v1/learning-outcomes/" + outcomeId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isNoContent());

        Integer active = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN is_active THEN 1 ELSE 0 END FROM academic_management.learning_outcome WHERE id = ?::uuid",
                Integer.class, outcomeId);
        assertThat(active).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_by FROM academic_management.learning_outcome WHERE id = ?::uuid",
                UUID.class, outcomeId)).isEqualTo(actorId);
    }
}
