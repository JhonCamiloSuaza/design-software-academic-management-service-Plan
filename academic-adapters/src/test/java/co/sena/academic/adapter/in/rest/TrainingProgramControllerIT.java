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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-ACAD-04 integration tests against real PostgreSQL (Testcontainers, no DB mocks).
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TrainingProgramControllerIT {

        @SuppressWarnings("resource")
        @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("academic_management_training_program_it")
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

    private String createProgram(UUID knowledgeNetworkId, String programCode) throws Exception {
        return mockMvc.perform(post("/training-programs")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledge_network_id":"%s",
                                  "program_code":"%s",
                                  "name":"Software Analysis and Development",
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
    void e1_createsTrainingProgramWithVersionOneAndRowVersionZero() throws Exception {
        UUID knowledgeNetworkId = seedKnowledgeNetwork();
        String code = "TP-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/training-programs")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledge_network_id":"%s",
                                  "program_code":"%s",
                                  "name":"Software Analysis and Development",
                                  "training_level":"TECHNOLOGIST",
                                  "total_hours":2640
                                }
                                """.formatted(knowledgeNetworkId, code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.knowledge_network_id").value(knowledgeNetworkId.toString()))
                .andExpect(jsonPath("$.program_code").value(code))
                .andExpect(jsonPath("$.training_level").value("TECHNOLOGIST"))
                .andExpect(jsonPath("$.total_hours").value(2640))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.row_version").value(0))
                .andExpect(jsonPath("$.is_active").value(true));
    }

    @Test
    void e1_returns409WhenProgramCodeAlreadyExistsGlobally() throws Exception {
        UUID firstKnowledgeNetworkId = seedKnowledgeNetwork();
        UUID secondKnowledgeNetworkId = seedKnowledgeNetwork();
        String code = "GLOBAL-" + UUID.randomUUID().toString().substring(0, 8);

        createProgram(firstKnowledgeNetworkId, code);

        mockMvc.perform(post("/training-programs")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledge_network_id":"%s",
                                  "program_code":"%s",
                                  "name":"Another program",
                                  "training_level":"TECHNICIAN",
                                  "total_hours":2200
                                }
                                """.formatted(secondKnowledgeNetworkId, code)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_PROGRAM_CODE_ALREADY_EXISTS"));
    }

    @Test
    void e1_returns404WhenKnowledgeNetworkDoesNotExist() throws Exception {
        mockMvc.perform(post("/training-programs")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledge_network_id":"%s",
                                  "program_code":"TP-NO-PARENT",
                                  "name":"Orphan program",
                                  "training_level":"TECHNICIAN",
                                  "total_hours":1000
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACAD_KNOWLEDGE_NETWORK_NOT_FOUND"));
    }

    @Test
    void e1_rejectsInvalidTrainingLevelAndNegativeTotalHours() throws Exception {
        UUID knowledgeNetworkId = seedKnowledgeNetwork();

        mockMvc.perform(post("/training-programs")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledge_network_id":"%s",
                                  "program_code":"TP-INVALID",
                                  "name":"Invalid",
                                  "training_level":"MASTER",
                                  "total_hours":-1
                                }
                                """.formatted(knowledgeNetworkId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_VALIDATION_ERROR"));
    }

    @Test
    void e1_requiresManagePermission() throws Exception {
        UUID knowledgeNetworkId = seedKnowledgeNetwork();

        mockMvc.perform(post("/training-programs")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledge_network_id":"%s",
                                  "program_code":"TP-403",
                                  "name":"Forbidden",
                                  "training_level":"AUXILIARY",
                                  "total_hours":100
                                }
                                """.formatted(knowledgeNetworkId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void e2_updatesVersionWithoutChangingExistingFichaSnapshot() throws Exception {
        UUID knowledgeNetworkId = seedKnowledgeNetwork();
        String createResponse = createProgram(
                knowledgeNetworkId,
                "SNAP-" + UUID.randomUUID().toString().substring(0, 8));
        UUID programId = UUID.fromString(JsonPath.read(createResponse, "$.id"));
        int rowVersion = JsonPath.read(createResponse, "$.row_version");

        UUID fichaId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO academic_management.enrollment_ficha "
                        + "(id, ficha_number, program_id, program_version, training_center_id, start_date, "
                        + "training_shift, training_modality, max_capacity, created_by, updated_by) "
                        + "VALUES (?::uuid, ?, ?::uuid, 1, ?::uuid, DATE '2026-02-01', "
                        + "'DAY', 'IN_PERSON', 30, ?::uuid, ?::uuid)",
                fichaId,
                "F-" + UUID.randomUUID().toString().substring(0, 8),
                programId,
                UUID.randomUUID(),
                actorId,
                actorId);

        mockMvc.perform(put("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":2,"is_active":false,"row_version":%d}
                                """.formatted(rowVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.is_active").value(false))
                .andExpect(jsonPath("$.row_version").value(rowVersion + 1));

        Integer frozenVersion = jdbcTemplate.queryForObject(
                "SELECT program_version FROM academic_management.enrollment_ficha WHERE id = ?::uuid",
                Integer.class,
                fichaId);
        assertThat(frozenVersion).isEqualTo(1);
    }

    @Test
    void e3_returns409WhenSentRowVersionIsStale() throws Exception {
        UUID knowledgeNetworkId = seedKnowledgeNetwork();
        String createResponse = createProgram(
                knowledgeNetworkId,
                "STALE-" + UUID.randomUUID().toString().substring(0, 8));
        String programId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"First update","row_version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.row_version").value(1));

        mockMvc.perform(put("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Stale update","row_version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROW_VERSION_MISMATCH"));
    }

    @Test
    void e3_twoRealConcurrentUpdatesProduceOneSuccessAndOneConflict() throws Exception {
        UUID knowledgeNetworkId = seedKnowledgeNetwork();
        String createResponse = createProgram(
                knowledgeNetworkId,
                "RACE-" + UUID.randomUUID().toString().substring(0, 8));
        String programId = JsonPath.read(createResponse, "$.id");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                int updateNumber = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return mockMvc.perform(put("/training-programs/" + programId)
                                    .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"name":"Concurrent update %d","row_version":0}
                                            """.formatted(updateNumber)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }

            ready.await();
            start.countDown();

            List<Integer> statuses = List.of(futures.get(0).get(), futures.get(1).get());
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void e4_getAndListReturnVersionAndApplyAllDeclaredFilters() throws Exception {
        UUID knowledgeNetworkId = seedKnowledgeNetwork();
        String targetCode = "FILTER-" + UUID.randomUUID().toString().substring(0, 8);
        String createResponse = createProgram(knowledgeNetworkId, targetCode);
        String programId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/training-programs/" + programId)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.row_version").value(0));

        mockMvc.perform(get("/training-programs")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .param("knowledge_network_id", knowledgeNetworkId.toString())
                        .param("program_code", targetCode)
                        .param("training_level", "TECHNOLOGIST")
                        .param("is_active", "true")
                        .param("page", "1")
                        .param("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].program_code").value(targetCode))
                .andExpect(jsonPath("$.data[0].version").value(1))
                .andExpect(jsonPath("$.pagination.page").value(1));
    }

    @Test
    void e4_requiresViewPermission() throws Exception {
        mockMvc.perform(get("/training-programs")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isForbidden());
    }
}