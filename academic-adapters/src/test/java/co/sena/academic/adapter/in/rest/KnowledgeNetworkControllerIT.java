package co.sena.academic.adapter.in.rest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import co.sena.academic.adapter.in.rest.security.JwtFeaturesConverter;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import java.util.List;
import java.util.UUID;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test against a real Postgres instance (Testcontainers, no
 * mocks) — required by the HU-ACAD-03 DoD. Covers E1-E3 (+ update) end
 * to end: HTTP -> controller -> use case -> JPA -> Postgres, including
 * the FK to tech_network and the application-level 409 for active
 * training_program dependents.
 *
 * jwtWithFeatures(...) mirrors JwtFeaturesConverter's own parsing, same
 * convention as TechLineControllerIT.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeNetworkControllerIT {

        @SuppressWarnings("resource")
        @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("academic_management_it")
            .withUsername("it_user")
            .withPassword("it_pass");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "https://example.invalid/jwks.json");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtFeaturesConverter jwtFeaturesConverter;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID actorId = UUID.randomUUID();

    private RequestPostProcessor jwtWithFeatures(UUID subject, String... features) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("features", List.of(features))
                .subject(subject.toString())
                .build();
        return authentication(jwtFeaturesConverter.convert(jwt));
    }

    @BeforeAll
    static void applySchema() throws Exception {
        postgres.copyFileToContainer(
                MountableFile.forClasspathResource("db/it-changelog.sql"),
                "/it-changelog.sql");
        postgres.execInContainer("psql", "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(), "-f", "/it-changelog.sql");
    }

    // ---------------------------------------------------------------------
    // Helpers: seed a tech_network (parent) directly via JDBC, since this
    // service does not own that aggregate/HU yet.
    // ---------------------------------------------------------------------

    private UUID seedTechNetwork() {
        UUID techLineId = UUID.randomUUID();
        UUID techNetworkId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO academic_management.tech_line (id, name, code, is_active, created_by, updated_by) " +
                        "VALUES (?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                techLineId, "Line-" + techLineId, "L-" + techLineId.toString().substring(0, 8), actorId, actorId);
        jdbcTemplate.update(
                "INSERT INTO academic_management.tech_network (id, tech_line_id, name, code, is_active, created_by, updated_by) " +
                        "VALUES (?::uuid, ?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                techNetworkId, techLineId, "Network-" + techNetworkId, "N-" + techNetworkId.toString().substring(0, 8), actorId, actorId);
        return techNetworkId;
    }

    private void seedActiveTrainingProgram(UUID knowledgeNetworkId) {
        jdbcTemplate.update(
                "INSERT INTO academic_management.training_program " +
                        "(id, knowledge_network_id, program_code, name, training_level, total_hours, is_active, created_by, updated_by) " +
                        "VALUES (?::uuid, ?::uuid, ?, ?, 'TECHNOLOGIST', 2640, TRUE, ?::uuid, ?::uuid)",
                UUID.randomUUID(), knowledgeNetworkId,
                "P-" + UUID.randomUUID().toString().substring(0, 8), "Program", actorId, actorId);
    }

    // ---- E1 ----
    @Test
    void e1_createsANetworkAndReturns201() throws Exception {
        UUID techNetworkId = seedTechNetwork();

        mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"Software Development","code":"RED-SW"}
                                """.formatted(techNetworkId)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.code").value("RED-SW"))
                        .andExpect(jsonPath("$.tech_network_id").value(techNetworkId.toString()))
                        .andExpect(jsonPath("$.is_active").value(true));
    }

    @Test
    void e1_returns404WhenTechNetworkIdDoesNotExist() throws Exception {
        UUID missingTechNetworkId = UUID.randomUUID();

        mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"Orphan","code":"RED-ORPHAN"}
                                """.formatted(missingTechNetworkId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACAD_TECH_NETWORK_NOT_FOUND"));
    }

    @Test
    void e1_returns409WhenCodeAlreadyExistsForTheSameTechNetwork() throws Exception {
        UUID techNetworkId = seedTechNetwork();

        mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"First","code":"RED-DUP"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"Duplicate","code":"RED-DUP"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_KNOWLEDGE_NETWORK_CODE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    @Test
    void e1_allowsTheSameCodeUnderADifferentTechNetwork() throws Exception {
        UUID techNetworkA = seedTechNetwork();
        UUID techNetworkB = seedTechNetwork();

        mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"A","code":"RED-SHARED"}
                                """.formatted(techNetworkA)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"B","code":"RED-SHARED"}
                                """.formatted(techNetworkB)))
                .andExpect(status().isCreated());
    }

    @Test
    void e1_returns403WhenActorOnlyHasViewFeature() throws Exception {
        UUID techNetworkId = seedTechNetwork();

        mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"Should not be created","code":"RED-403"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void e1_returns400WhenNameIsBlank() throws Exception {
        UUID techNetworkId = seedTechNetwork();

        mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"","code":"RED-400"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void e1_returns400WhenTechNetworkIdIsMissing() throws Exception {
        mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"No parent","code":"RED-NO-PARENT"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ---- E2 ----
        @Test
        void e2_listsFilteredByTechNetworkId() throws Exception {
        UUID techNetworkId = seedTechNetwork();
        mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"Listable","code":"RED-LIST"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .param("tech_network_id", techNetworkId.toString())
                        .param("page", "1").param("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].tech_network_id").value(techNetworkId.toString()))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.page_size").value(20));
        }

        @Test
        void e2_listsAllWhenTechNetworkIdQueryParamIsMissing() throws Exception {
        mockMvc.perform(get("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        }

    @Test
    void e2_returns403WhenActorHasNoAcademicFeatureAtAll() throws Exception {
        UUID techNetworkId = seedTechNetwork();

        mockMvc.perform(get("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "SCHEDULING_MANAGE:GLOBAL"))
                        .param("tech_network_id", techNetworkId.toString()))
                .andExpect(status().isForbidden());
    }

    // ---- update ----
    @Test
    void update_updatesAnExistingNetwork() throws Exception {
        UUID techNetworkId = seedTechNetwork();
        String createResponse = mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"Original","code":"RED-UPD"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/knowledge-networks/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"Updated","code":"RED-UPD-2","is_active":true}
                                """.formatted(techNetworkId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RED-UPD-2"));
    }

    @Test
    void update_returns404WhenItDoesNotExist() throws Exception {
        UUID techNetworkId = seedTechNetwork();
        mockMvc.perform(put("/knowledge-networks/" + UUID.randomUUID())
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"N","code":"NOPE","is_active":true}
                                """.formatted(techNetworkId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_rejectsBlankName() throws Exception {
        UUID techNetworkId = seedTechNetwork();
        String createResponse = mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"Original","code":"RED-BLANK"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/knowledge-networks/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_rejectsBlankCode() throws Exception {
        UUID techNetworkId = seedTechNetwork();
        String createResponse = mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"Original","code":"RED-BLANK-2"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/knowledge-networks/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ---- E3 ----
    @Test
    void e3_deactivatesLogicallyWhenThereAreNoActiveTrainingProgramDependents() throws Exception {
        UUID techNetworkId = seedTechNetwork();
        String createResponse = mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"To deactivate","code":"RED-DEL"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(delete("/knowledge-networks/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isNoContent());

        Boolean active = jdbcTemplate.queryForObject(
                "SELECT is_active FROM academic_management.knowledge_network WHERE id = ?::uuid",
                Boolean.class, id);
        org.assertj.core.api.Assertions.assertThat(active).isFalse();
    }

    @Test
    void e3_returns409WhenThereAreActiveTrainingProgramDependents() throws Exception {
        UUID techNetworkId = seedTechNetwork();
        String createResponse = mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"Has dependents","code":"RED-BUSY"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");
        seedActiveTrainingProgram(UUID.fromString(id));

        mockMvc.perform(delete("/knowledge-networks/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_KNOWLEDGE_NETWORK_HAS_ACTIVE_TRAINING_PROGRAMS"));

        Boolean active = jdbcTemplate.queryForObject(
                "SELECT is_active FROM academic_management.knowledge_network WHERE id = ?::uuid",
                Boolean.class, id);
        org.assertj.core.api.Assertions.assertThat(active).isTrue();
    }

    @Test
    void e3_returns404WhenItDoesNotExist() throws Exception {
        mockMvc.perform(delete("/knowledge-networks/" + UUID.randomUUID())
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isNotFound());
    }

    @Test
    void e3_returns403WhenActorOnlyHasViewFeature() throws Exception {
        UUID techNetworkId = seedTechNetwork();
        String createResponse = mockMvc.perform(post("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_network_id":"%s","name":"For 403 delete","code":"RED-403-DEL"}
                                """.formatted(techNetworkId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(delete("/knowledge-networks/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isForbidden());
    }
    @Test
        void list_rejectsPageLessThanOne() throws Exception {
        mockMvc.perform(get("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .param("page", "0"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void list_rejectsPageSizeAboveHundred() throws Exception {
        mockMvc.perform(get("/knowledge-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .param("page_size", "101"))
                .andExpect(status().isBadRequest());
        }
}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       