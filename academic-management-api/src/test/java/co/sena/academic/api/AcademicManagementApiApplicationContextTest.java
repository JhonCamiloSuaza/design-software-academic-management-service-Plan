package co.sena.academic.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
* Full-context (*context-loads*) test, required by every Java service per 
* 10-devops/ci-cd.md and 11-quality/testing-strategy.md: 
* 
* "Every Java service must have at least one context @SpringBootTest 
* complete (context-loads) that boots the real web environment, in addition 
* of the @WebMvcTest/units per controller." 
* 
* Motivated by the real "ambiguous mapping" incident in iam-service: two 
* @RestController beans implementing the same generated OpenAPI interface 
* broke Spring Boot startup, and neither @WebMvcTest nor unit tests caught 
* it, because they load a single isolated controller and never boot the 
* full ApplicationContext. Mirrors iam-service's own remediation 
* (IamApiApplicationContextTest). 
* 
* academic-management-service only has ONE @RestController (TechLineController) 
* today, so this test currently guards against regressions (wiring breaks, 
* duplicate beans, missing @Bean definitions) rather than an existing bug — 
* but it is the mandatory guardrail per the org-wide rule, and must keep 
* passing as more controllers/use cases are added to this service.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcademicManagementApiApplicationContextTest {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("academic_management_context_it")
            .withUsername("it_user")
            .withPassword("it_pass");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // No liquibase/ddl here on purpose: this test only needs the JDBC 
        // connection to be reachable so the JPA/Hibernate validate step and 
        // the full ApplicationContext can boot; it does not exercise any 
        // endpoint or query, that is TechLineControllerIT's job.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.rabbitmq.dynamic", () -> "false");
    }

    @Test
    void contextLoads() {
        // Intentionally empty: the assertion is that the Spring context
        // (web, security filter chain, JPA repositories, all @Bean wiring
        // across academic-core/academic-adapters/academic-management-api)
        // boots without ambiguous mappings, missing beans, or broken wiring.
    }
}