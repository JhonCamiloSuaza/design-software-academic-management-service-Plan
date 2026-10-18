package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.rest.security.JwtFeaturesConverter;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test against real Postgres (Testcontainers, no mocks) —
 * required by HU-ACAD-02's DoD. Covers E1-E3 end to end, including the
 * tech_line existence/active validation that HU-ACAD-02 adds on top of
 * HU-ACAD-01's pattern.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TechNetworkControllerIT {

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

    /** Helper: inserts an active tech_line directly and returns its id, so tests don't depend on HU-ACAD-01's endpoint. */
    private UUID seedActiveTechLine(String code) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO academic_management.tech_line (id, name, code, is_active, created_by, updated_by)
                VALUES (?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)
                """, id.toString(), "Line " + code, code, actorId.toString(), actorId.toString());
        return id;
    }

    private UUID seedInactiveTechLine(String code) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO academic_management.tech_line (id, name, code, is_active, created_by, updated_by)
                VALUES (?::uuid, ?, ?, FALSE, ?::uuid, ?::uuid)
                """, id.toString(), "Inactive Line " + code, code, actorId.toString(), actorId.toString());
        return id;
    }

    @Test
    void e1_createsANetworkAndReturns201() throws Exception {
        UUID techLineId = seedActiveTechLine("TL-E1-1");

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"Software Development","code":"TN-SW"}
                                """.formatted(techLineId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TN-SW"))
                .andExpect(jsonPath("$.tech_line_id").value(techLineId.toString()));
    }

    @Test
    void e1_returns404WhenTechLineDoesNotExist() throws Exception {
        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"Ghost","code":"TN-404"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void e1_returns409WhenTechLineIsNotActive() throws Exception {
        UUID inactiveLineId = seedInactiveTechLine("TL-INACT-1");

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"Under inactive line","code":"TN-INACT"}
                                """.formatted(inactiveLineId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_TECH_LINE_NOT_ACTIVE"));
    }

    @Test
    void e1_returns409WhenCodeAlreadyExistsUnderTheSameLine() throws Exception {
        UUID techLineId = seedActiveTechLine("TL-E1-2");

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"First","code":"TN-DUP"}
                                """.formatted(techLineId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"Duplicate","code":"TN-DUP"}
                                """.formatted(techLineId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACAD_TECH_NETWORK_CODE_ALREADY_EXISTS"));
    }

    @Test
    void e1_allowsTheSameCodeUnderADifferentLine() throws Exception {
        UUID lineA = seedActiveTechLine("TL-A");
        UUID lineB = seedActiveTechLine("TL-B");

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"Under A","code":"TN-SAME"}
                                """.formatted(lineA)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"Under B","code":"TN-SAME"}
                                """.formatted(lineB)))
                .andExpect(status().isCreated());
    }

    @Test
    void e1_returns403WhenActorOnlyHasViewFeature() throws Exception {
        UUID techLineId = seedActiveTechLine("TL-403-1");

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"Should not be created","code":"TN-403"}
                                """.formatted(techLineId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void e1_returns400WhenNameIsBlank() throws Exception {
        UUID techLineId = seedActiveTechLine("TL-400-1");

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"","code":"TN-400"}
                                """.formatted(techLineId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void e2_listsFilteredByTechLineId() throws Exception {
        UUID lineA = seedActiveTechLine("TL-LIST-A");
        UUID lineB = seedActiveTechLine("TL-LIST-B");

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"In A","code":"TN-LIST-A"}
                                """.formatted(lineA)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"In B","code":"TN-LIST-B"}
                                """.formatted(lineB)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .param("tech_line_id", lineA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.code == 'TN-LIST-B')]").doesNotExist());
    }
    @Test
        void e2_listsAllTechNetworksWhenNoFilterIsProvided() throws Exception {
        // Counts the current state first (this test class shares one Testcontainers
        // instance across methods, so other tests may have already inserted rows).
        long totalBefore = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM academic_management.tech_network", Long.class);

        UUID lineA = seedActiveTechLine("TL-ALL-A");
        UUID lineB = seedActiveTechLine("TL-ALL-B");

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"All A","code":"TN-ALL-A"}
                                """.formatted(lineA)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"All B","code":"TN-ALL-B"}
                                """.formatted(lineB)))
                .andExpect(status().isCreated());

        // No "tech_line_id" param at all -> must return every network, from both lines.
        mockMvc.perform(get("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .param("page", "0")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(totalBefore + 2))
                .andExpect(jsonPath("$.content[?(@.code == 'TN-ALL-A')]").exists())
                .andExpect(jsonPath("$.content[?(@.code == 'TN-ALL-B')]").exists());
        }

    @Test
    void e2_returns403WhenActorHasNoAcademicFeatureAtAll() throws Exception {
        mockMvc.perform(get("/tech-networks")
                        .with(jwtWithFeatures(actorId, "SCHEDULING_MANAGE:GLOBAL")))
                .andExpect(status().isForbidden());
    }

    @Test
    void e3_updatesAnExistingNetwork() throws Exception {
        UUID techLineId = seedActiveTechLine("TL-UPD");

        String createResponse = mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"Original","code":"TN-UPD"}
                                """.formatted(techLineId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/tech-networks/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated","code":"TN-UPD-2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TN-UPD-2"));
    }

    @Test
    void e3_returns404WhenItDoesNotExist() throws Exception {
        mockMvc.perform(put("/tech-networks/" + UUID.randomUUID())
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"N","code":"NOPE"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void e3_deactivatesLogicallyWithoutPhysicallyDeletingIt() throws Exception {
        UUID techLineId = seedActiveTechLine("TL-DEL");

        String createResponse = mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"To deactivate","code":"TN-DEL"}
                                """.formatted(techLineId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(delete("/tech-networks/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isNoContent());

        Boolean active = jdbcTemplate.queryForObject(
                "SELECT is_active FROM academic_management.tech_network WHERE id = ?::uuid",
                Boolean.class, id);
        org.assertj.core.api.Assertions.assertThat(active).isFalse();
    }

    @Test
    void e3_returns403WhenActorOnlyHasViewFeature() throws Exception {
        UUID techLineId = seedActiveTechLine("TL-403-2");

        String createResponse = mockMvc.perform(post("/tech-networks")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tech_line_id":"%s","name":"For 403","code":"TN-403-2"}
                                """.formatted(techLineId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(delete("/tech-networks/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isForbidden());
    }
}