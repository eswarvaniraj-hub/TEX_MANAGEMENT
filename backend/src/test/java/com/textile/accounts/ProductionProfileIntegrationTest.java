package com.textile.accounts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("prod")
public class ProductionProfileIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("Application starts up cleanly under 'prod' profile with production YAML configuration loaded")
    void contextLoadsWithProdProfile() {
        assertThat(environment.getActiveProfiles()).contains("prod");
        assertThat(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class)).isTrue();
        assertThat(environment.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
        assertThat(environment.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
    }
}
