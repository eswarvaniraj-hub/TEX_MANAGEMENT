package com.textile.accounts;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // Single shared singleton Testcontainers container across all integration tests
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER;

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("textile_accounts_test")
                .withUsername("test_user")
                .withPassword("test_password");
            POSTGRESQL_CONTAINER.start();
        } else {
            POSTGRESQL_CONTAINER = null;
        }
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        if (POSTGRESQL_CONTAINER != null && POSTGRESQL_CONTAINER.isRunning()) {
            registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
            registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        } else {
            // Fallback for local environments where Docker is not installed
            String port = System.getProperty("local.pg.port", "5433");
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:" + port + "/postgres");
            registry.add("spring.datasource.username", () -> "postgres");
            registry.add("spring.datasource.password", () -> "");
        }
    }
}
