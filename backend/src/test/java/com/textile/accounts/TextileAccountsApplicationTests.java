package com.textile.accounts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class TextileAccountsApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Application context loads and Flyway V1 migrations execute successfully")
    void contextLoadsAndFlywayAppliesCleanly() {
        // Assert that Flyway migration V1 applied cleanly
        Integer flywayCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class
        );
        assertThat(flywayCount).isNotNull().isGreaterThanOrEqualTo(1);

        // Assert that the seed tables and rows exist
        Integer stateCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM gst_state", Integer.class
        );
        assertThat(stateCount).isEqualTo(38);

        Integer fyCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM financial_year", Integer.class
        );
        assertThat(fyCount).isEqualTo(2);

        // Assert audit_event table is accessible
        Integer auditCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_event", Integer.class
        );
        assertThat(auditCount).isNotNull().isGreaterThanOrEqualTo(0);

        // Assert Spring Session tables exist and are functional
        Integer sessionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM spring_session", Integer.class
        );
        assertThat(sessionCount).isNotNull().isGreaterThanOrEqualTo(0);
    }
}
