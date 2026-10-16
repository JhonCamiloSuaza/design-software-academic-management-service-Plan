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

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test against a real Postgres instance (Testcontainers, no
 * mocks) — required by the HU-ACAD-01 DoD. Covers E1-E4 end to end:
 * HTTP -> controller -> use case -> JPA -> Postgres.
 *
 * jwtWithFeatures(...) simulates a real iam-service JWT with a "features"
 * claim AND wires the resulting GrantedAuthority list explicitly (mirroring
 * JwtFeaturesConverter's own parsing), because
 * SecurityMockMvcRequestPostProcessors.jwt() does NOT invoke the app's
 * registered JwtAuthenticationConverter bean on its own.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TechLineControllerIT {

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

    @Test
    void e1_createsALineAndReturns201() throws Exception {
        mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Information Technologies","code":"TIC-IT-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TIC-IT-1"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void e1_returns409WhenCodeAlreadyExists() throws Exception {
        mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"First","code":"TIC-DUP"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Duplicate","code":"TIC-DUP"}
                                """))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.error.code").value("ACAD_TECH_LINE_CODE_ALREADY_EXISTS"))
                        .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    @Test
    void e2_listsAndFetchesALine() throws Exception {
        mockMvc.perform(get("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void e3_updatesAnExistingLine() throws Exception {
        String createResponse = mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Original","code":"TIC-UPD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/tech-lines/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated","code":"TIC-UPD-2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TIC-UPD-2"));
    }

    @Test
    void e3_returns404WhenItDoesNotExist() throws Exception {
        mockMvc.perform(put("/tech-lines/" + UUID.randomUUID())
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"N","code":"NOPE"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void e4_deactivatesLogicallyWithoutPhysicallyDeletingIt() throws Exception {
        String createResponse = mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"To deactivate","code":"TIC-DEL"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(delete("/tech-lines/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL")))
                .andExpect(status().isNoContent());

        Boolean active = jdbcTemplate.queryForObject(
                "SELECT is_active FROM academic_management.tech_line WHERE id = ?::uuid",
                Boolean.class, id);
        org.assertj.core.api.Assertions.assertThat(active).isFalse();
    }

    // ---------------------------------------------------------------------
        // RBAC (403) — insufficient feature. Closes the DoD gap: the HU requires
        // ACADEMIC_PROGRAM_MANAGE for E1/E3/E4 and ACADEMIC_PROGRAM_VIEW for E2;

        // It was necessary to prove that an actor WITHOUT this feature is indeed rejected
        // (it is not enough to prove the successful path with the correct feature).
    // ---------------------------------------------------------------------

    @Test
    void e1_returns403WhenActorOnlyHasViewFeature() throws Exception {
        mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Should not be created","code":"TIC-403-1"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void e2_returns403WhenActorHasNoAcademicFeatureAtAll() throws Exception {
        mockMvc.perform(get("/tech-lines")
                        .with(jwtWithFeatures(actorId, "SCHEDULING_MANAGE:GLOBAL")))
                .andExpect(status().isForbidden());
    }

    @Test
    void e3_returns403WhenActorOnlyHasViewFeature() throws Exception {
        String createResponse = mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"For 403 update","code":"TIC-403-2"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/tech-lines/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Attempted update","code":"TIC-403-2B"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void e4_returns403WhenActorOnlyHasViewFeature() throws Exception {
        String createResponse = mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"For 403 delete","code":"TIC-403-3"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(delete("/tech-lines/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isForbidden());

        Boolean active = jdbcTemplate.queryForObject(
                "SELECT is_active FROM academic_management.tech_line WHERE id = ?::uuid",
                Boolean.class, id);
        org.assertj.core.api.Assertions.assertThat(active).isTrue();
    }

    // ---------------------------------------------------------------------
        // Validation (400) — name/code blank or out of length. Closes the
        // other DoD gap: the @NotBlank/@Size DTOs lacked IT to
        // confirm the actual 400 from Spring Validation against the HTTP endpoint.
    // ---------------------------------------------------------------------

    @Test
    void e1_returns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","code":"TIC-400-1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void e1_returns400WhenCodeIsBlank() throws Exception {
        mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Missing code","code":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void e1_returns400WhenCodeExceedsMaxLength() throws Exception {
        String tooLongCode = "A".repeat(21); // VARCHAR(20) en el DDL real

        mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Code too long","code":"%s"}
                                """.formatted(tooLongCode)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void e3_returns400WhenNameIsBlank() throws Exception {
        String createResponse = mockMvc.perform(post("/tech-lines")
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"For 400 update","code":"TIC-400-2"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/tech-lines/" + id)
                        .with(jwtWithFeatures(actorId, "ACADEMIC_PROGRAM_MANAGE:GLOBAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","code":"TIC-400-2"}
                                """))
                .andExpect(status().isBadRequest());
    }
}