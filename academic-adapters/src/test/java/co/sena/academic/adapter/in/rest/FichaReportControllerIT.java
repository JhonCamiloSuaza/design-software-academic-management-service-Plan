package co.sena.academic.adapter.in.rest;

import co.sena.academic.adapter.in.rest.security.JwtFeaturesConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class FichaReportControllerIT {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("academic_management_reports_it")
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

    private final UUID userId = UUID.randomUUID();

    @BeforeAll
    static void applySchema() throws Exception {
        postgres.copyFileToContainer(
                MountableFile.forClasspathResource("db/it-changelog.sql"),
                "/it-changelog.sql");
        postgres.execInContainer(
                "psql", "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(), "-f", "/it-changelog.sql");
    }

    @Test
    void hu14_projectsEnrollmentCapacityAndProgramDataWithFiltersAndPagination() throws Exception {
        Program firstProgram = seedProgram("PRG-14-A", "Program for HU-ACAD-14 A");
        Program secondProgram = seedProgram("PRG-14-B", "Program for HU-ACAD-14 B");
        UUID centerId = UUID.randomUUID();
        UUID otherCenterId = UUID.randomUUID();
        UUID expectedFicha = insertFicha(
                firstProgram.id(), centerId, "F-14-EXPECTED", "EXECUTION",
                LocalDate.of(2026, 3, 10), LocalDate.of(2027, 3, 10),
                "NIGHT", "HYBRID", 28, null);
        insertFicha(
                firstProgram.id(), centerId, "F-14-EXCLUDED-BY-TO", "EXECUTION",
                LocalDate.of(2026, 4, 1), LocalDate.of(2027, 4, 1),
                "DAY", "IN_PERSON", 30, null);
        insertFicha(
                secondProgram.id(), otherCenterId, "F-14-OTHER-PROGRAM", "EXECUTION",
                LocalDate.of(2026, 3, 10), LocalDate.of(2027, 3, 10),
                "DAY", "VIRTUAL", 31, null);

        mockMvc.perform(get("/api/v1/reports/enrollment-by-ficha")
                        .param("training_center_id", centerId.toString())
                        .param("program_id", firstProgram.id().toString())
                        .param("status", "EXECUTION")
                        .param("from", "2026-03-01")
                        .param("to", "2026-04-01")
                        .param("page", "1")
                        .param("page_size", "1")
                        .param("sort", "start_date:asc")
                        .with(jwtWithFeatures("ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ficha_id").value(expectedFicha.toString()))
                .andExpect(jsonPath("$.data[0].ficha_number").value("F-14-EXPECTED"))
                .andExpect(jsonPath("$.data[0].program_id").value(firstProgram.id().toString()))
                .andExpect(jsonPath("$.data[0].program_code").value(firstProgram.code()))
                .andExpect(jsonPath("$.data[0].program_name").value(firstProgram.name()))
                .andExpect(jsonPath("$.data[0].training_center_id").value(centerId.toString()))
                .andExpect(jsonPath("$.data[0].status").value("EXECUTION"))
                .andExpect(jsonPath("$.data[0].max_capacity").value(28))
                .andExpect(jsonPath("$.data[0].training_shift").value("NIGHT"))
                .andExpect(jsonPath("$.data[0].training_modality").value("HYBRID"))
                .andExpect(jsonPath("$.data[0].start_date").value("2026-03-10"))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.page_size").value(1))
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.pagination.total_pages").value(1));
    }

    @Test
    void hu14_forcesTrainingCenterScope() throws Exception {
        Program program = seedProgram("PRG-14-SCOPE", "Program scope");
        UUID ownCenter = UUID.randomUUID();
        UUID otherCenter = UUID.randomUUID();
        UUID ownFicha = insertFicha(
                program.id(), ownCenter, "F-14-OWN", "EXECUTION",
                LocalDate.of(2026, 5, 1), LocalDate.of(2027, 5, 1),
                "DAY", "IN_PERSON", 30, null);
        insertFicha(
                program.id(), otherCenter, "F-14-OTHER", "EXECUTION",
                LocalDate.of(2026, 5, 1), LocalDate.of(2027, 5, 1),
                "DAY", "IN_PERSON", 30, null);

        mockMvc.perform(get("/api/v1/reports/enrollment-by-ficha")
                        .with(jwtWithClaims(ownCenter, "ACADEMIC_FICHA_VIEW:TRAINING_CENTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.data[0].ficha_id").value(ownFicha.toString()));

        mockMvc.perform(get("/api/v1/reports/enrollment-by-ficha")
                        .param("training_center_id", otherCenter.toString())
                        .with(jwtWithClaims(ownCenter, "ACADEMIC_FICHA_VIEW:TRAINING_CENTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.pagination.total_items").value(0));
    }

    @Test
    void hu15_calculatesOnTimeDelayedAndUnknownExpectedEndDate() throws Exception {
        Program program = seedProgram("PRG-15", "Program for HU-ACAD-15");
        UUID centerId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        UUID delayedFicha = insertFicha(
                program.id(), centerId, "F-15-DELAYED", "EXECUTION",
                today.minusDays(20), today.minusDays(1), "DAY", "IN_PERSON", 30, null);
        UUID onTimeFicha = insertFicha(
                program.id(), centerId, "F-15-ON-TIME", "EXECUTION",
                today.minusDays(10), today.plusDays(10), "DAY", "IN_PERSON", 30, null);
        UUID unknownEndFicha = insertFicha(
                program.id(), centerId, "F-15-NO-EXPECTED-END", "EXECUTION",
                today.minusDays(5), null, "DAY", "IN_PERSON", 30, null);

        mockMvc.perform(get("/api/v1/reports/ficha-progress")
                        .param("training_center_id", centerId.toString())
                        .param("program_id", program.id().toString())
                        .param("sort", "start_date:asc")
                        .with(jwtWithFeatures("ACADEMIC_FICHA_VIEW:GLOBAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].ficha_id").value(delayedFicha.toString()))
                .andExpect(jsonPath("$.data[0].days_elapsed").value(20))
                .andExpect(jsonPath("$.data[0].days_remaining").value(-1))
                .andExpect(jsonPath("$.data[0].is_delayed").value(true))
                .andExpect(jsonPath("$.data[1].ficha_id").value(onTimeFicha.toString()))
                .andExpect(jsonPath("$.data[1].days_elapsed").value(10))
                .andExpect(jsonPath("$.data[1].days_remaining").value(10))
                .andExpect(jsonPath("$.data[1].is_delayed").value(false))
                .andExpect(jsonPath("$.data[2].ficha_id").value(unknownEndFicha.toString()))
                .andExpect(jsonPath("$.data[2].days_elapsed").value(5))
                .andExpect(jsonPath("$.data[2].expected_end_date").value(nullValue()))
                .andExpect(jsonPath("$.data[2].days_remaining").value(nullValue()))
                .andExpect(jsonPath("$.data[2].is_delayed").value(false));
    }

    @Test
    void reportsRequireFichaViewFeature() throws Exception {
        mockMvc.perform(get("/api/v1/reports/enrollment-by-ficha"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/reports/ficha-progress")
                        .with(jwtWithFeatures("ACADEMIC_PROGRAM_VIEW:GLOBAL")))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor jwtWithFeatures(String... features) {
        return jwtWithClaims(null, features);
    }

    private RequestPostProcessor jwtWithClaims(UUID trainingCenterId, String... features) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("features", List.of(features))
                .subject(userId.toString());
        if (trainingCenterId != null) {
            builder.claim("training_center_id", trainingCenterId.toString());
        }
        return authentication(jwtFeaturesConverter.convert(builder.build()));
    }

    private Program seedProgram(String code, String name) {
        UUID techLineId = UUID.randomUUID();
        UUID techNetworkId = UUID.randomUUID();
        UUID knowledgeNetworkId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        String suffix = programId.toString().substring(0, 8);

        jdbcTemplate.update(
                "INSERT INTO academic_management.tech_line "
                        + "(id, name, code, is_active, created_by, updated_by) VALUES (?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                techLineId, "Line-" + suffix, "L-" + suffix, userId, userId);
        jdbcTemplate.update(
                "INSERT INTO academic_management.tech_network "
                        + "(id, tech_line_id, name, code, is_active, created_by, updated_by) VALUES (?::uuid, ?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                techNetworkId, techLineId, "Network-" + suffix, "N-" + suffix, userId, userId);
        jdbcTemplate.update(
                "INSERT INTO academic_management.knowledge_network "
                        + "(id, tech_network_id, name, code, is_active, created_by, updated_by) VALUES (?::uuid, ?::uuid, ?, ?, TRUE, ?::uuid, ?::uuid)",
                knowledgeNetworkId, techNetworkId, "Knowledge-" + suffix, "K-" + suffix, userId, userId);
        jdbcTemplate.update(
                "INSERT INTO academic_management.training_program "
                        + "(id, knowledge_network_id, program_code, name, training_level, total_hours, version, is_active, row_version, created_by, updated_by) "
                        + "VALUES (?::uuid, ?::uuid, ?, ?, 'TECHNOLOGIST', 2640, 1, TRUE, 0, ?::uuid, ?::uuid)",
                programId, knowledgeNetworkId, code, name, userId, userId);
        return new Program(programId, code, name);
    }

    private UUID insertFicha(UUID programId,
                             UUID trainingCenterId,
                             String fichaNumber,
                             String status,
                             LocalDate startDate,
                             LocalDate expectedEndDate,
                             String shift,
                             String modality,
                             int maxCapacity,
                             LocalDate actualEndDate) {
        UUID fichaId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO academic_management.enrollment_ficha "
                        + "(id, ficha_number, program_id, program_version, training_center_id, status, start_date, expected_end_date, actual_end_date, "
                        + "training_shift, training_modality, max_capacity, row_version, created_by, updated_by) "
                        + "VALUES (?::uuid, ?, ?::uuid, 1, ?::uuid, ?, ?, ?, ?, ?, ?, ?, 0, ?::uuid, ?::uuid)",
                fichaId, fichaNumber, programId, trainingCenterId, status, startDate, expectedEndDate,
                actualEndDate, shift, modality, maxCapacity, userId, userId);
        return fichaId;
    }

    private record Program(UUID id, String code, String name) {
    }
}
