package com.textile.accounts;

import com.textile.accounts.common.config.DatabaseUrlEnvironmentPostProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    @DisplayName("Verify post processor implements Ordered and has LOWEST_PRECEDENCE")
    void verifyOrder() {
        assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    @DisplayName("Parses standard cloud postgres:// connection string into JDBC and Flyway properties")
    void parsesCloudPostgresUrl() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgres://test_user:secret_pwd@dpg-abc-123.render.com:5432/textile_db");

        processor.postProcessEnvironment(env, new SpringApplication());

        // DataSource properties
        assertThat(env.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://dpg-abc-123.render.com:5432/textile_db");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("test_user");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("secret_pwd");

        // Flyway properties (guarantees Flyway uses same connection)
        assertThat(env.getProperty("spring.flyway.url"))
            .isEqualTo("jdbc:postgresql://dpg-abc-123.render.com:5432/textile_db");
        assertThat(env.getProperty("spring.flyway.user")).isEqualTo("test_user");
        assertThat(env.getProperty("spring.flyway.password")).isEqualTo("secret_pwd");

        // Uppercase environment variable aliases
        assertThat(env.getProperty("SPRING_DATASOURCE_URL"))
            .isEqualTo("jdbc:postgresql://dpg-abc-123.render.com:5432/textile_db");
        assertThat(env.getProperty("SPRING_DATASOURCE_USERNAME")).isEqualTo("test_user");
        assertThat(env.getProperty("SPRING_DATASOURCE_PASSWORD")).isEqualTo("secret_pwd");
    }

    @Test
    @DisplayName("Parses postgresql:// URL with query parameters like sslmode=require")
    void parsesUrlWithQueryParams() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgresql://render_user:p@ss123@dpg-xyz.singapore-postgres.render.com:5432/textile_db?sslmode=require");

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://dpg-xyz.singapore-postgres.render.com:5432/textile_db?sslmode=require");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("render_user");
    }

    @Test
    @DisplayName("Correctly decodes URL-encoded passwords (e.g. pass%40word%23)")
    void decodesUrlEncodedCredentials() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgres://user_admin:pass%40word%23123@host:5432/testdb");

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("pass@word#123");
    }

    @Test
    @DisplayName("Fallback parser handles unencoded @ character in password")
    void fallbackHandlesUnencodedSpecialChars() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgres://user_admin:secret@pass@host.internal:5432/testdb");

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://host.internal:5432/testdb");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("user_admin");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("secret@pass");
    }

    @Test
    @DisplayName("Accepts already-formatted jdbc:postgresql URLs directly")
    void acceptsDirectJdbcUrl() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "jdbc:postgresql://custom-host:5432/custom_db");

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://custom-host:5432/custom_db");
        assertThat(env.getProperty("spring.flyway.url"))
            .isEqualTo("jdbc:postgresql://custom-host:5432/custom_db");
    }

    @Test
    @DisplayName("Cleans unexpanded literal ${...} environment placeholders")
    void cleansLiteralPlaceholders() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.username", "${SPRING_DATASOURCE_USERNAME}");
        env.setProperty("SPRING_DATASOURCE_USERNAME", "${SPRING_DATASOURCE_USERNAME}");
        env.setProperty("POSTGRES_USER", "real_db_user");

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("real_db_user");
        assertThat(env.getProperty("SPRING_DATASOURCE_USERNAME")).isEqualTo("real_db_user");
    }
}
