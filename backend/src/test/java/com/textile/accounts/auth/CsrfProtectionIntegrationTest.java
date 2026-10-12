package com.textile.accounts.auth;

import com.textile.accounts.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class CsrfProtectionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/auth/csrf returns token and sets XSRF-TOKEN cookie")
    void testGetCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andReturn();

        Cookie xsrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();
        assertThat(xsrfCookie.getValue()).isNotEmpty();
        assertThat(xsrfCookie.isHttpOnly()).isFalse(); // SPA must be able to read it
    }

    @Test
    @DisplayName("State-changing POST without CSRF token is rejected with 403 Forbidden")
    void testPostWithoutCsrfRejected() throws Exception {
        String loginJson = "{\"username\":\"owner\",\"password\":\"SomePassword123!\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("State-changing POST with invalid CSRF token is rejected with 403 Forbidden")
    void testPostWithInvalidCsrfRejected() throws Exception {
        String loginJson = "{\"username\":\"owner\",\"password\":\"SomePassword123!\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-XSRF-TOKEN", "invalid-token-123")
                        .cookie(new Cookie("XSRF-TOKEN", "different-cookie-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("State-changing POST with valid XSRF-TOKEN header passes CSRF verification")
    void testPostWithValidCsrfAccepted() throws Exception {
        // First get the CSRF token from the server
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();
        String token = xsrfCookie.getValue();

        // Perform login with the matching cookie and header
        String loginJson = "{\"username\":\"owner\",\"password\":\"InvalidPassword123!\"}";
        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(xsrfCookie)
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                // Passes CSRF (not 403), proceeds to authentication failure (401)
                .andExpect(status().isUnauthorized());
    }
}
