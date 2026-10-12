package com.textile.accounts.auth;

import com.textile.accounts.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class EndpointSweepSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/health",
            "/api/v1/auth/login",
            "/api/v1/auth/csrf",
            "/actuator/health",
            "/error"
    );

    @Test
    @DisplayName("Endpoint sweep: all non-public mapped endpoints return 401 when accessed without credentials")
    void sweepAllEndpointsForAuthenticationRequirement() throws Exception {
        Set<RequestMappingInfo> mappings = handlerMapping.getHandlerMethods().keySet();
        assertThat(mappings).isNotEmpty();

        for (RequestMappingInfo mapping : mappings) {
            Set<String> patterns = mapping.getDirectPaths();
            if (patterns.isEmpty() && mapping.getPathPatternsCondition() != null) {
                patterns = mapping.getPathPatternsCondition().getPatternValues();
            }

            for (String path : patterns) {
                if (PUBLIC_PATHS.contains(path)) {
                    continue;
                }

                // If path has path variables (e.g. {id}), replace with 1 for test
                String testPath = path.replaceAll("\\{[^}]+\\}", "1");

                // Test GET
                mockMvc.perform(get(testPath))
                        .andExpect(status().isUnauthorized());

                // Test POST with CSRF token (so it doesn't fail on CSRF before auth)
                mockMvc.perform(post(testPath).with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Test
    @DisplayName("Assert no registration endpoints exist in the system (returns 401 or 404)")
    void assertNoRegistrationEndpoints() throws Exception {
        String[] forbiddenRoutes = {
                "/register",
                "/api/register",
                "/api/v1/register",
                "/signup",
                "/api/signup",
                "/api/v1/signup",
                "/api/v1/auth/register",
                "/api/v1/auth/signup"
        };

        for (String route : forbiddenRoutes) {
            // Unauthenticated POST should never succeed with 200 or 201
            mockMvc.perform(post(route).with(csrf()))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertThat(status)
                                .withFailMessage("Endpoint %s unexpectedly allowed access with status %d", route, status)
                                .isIn(401, 404, 405);
                    });
        }
    }
}
