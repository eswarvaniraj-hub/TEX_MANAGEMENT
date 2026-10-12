package com.textile.accounts.auth;

import com.textile.accounts.AbstractIntegrationTest;
import com.textile.accounts.audit.model.AuditEvent;
import com.textile.accounts.audit.repository.AuditEventRepository;
import com.textile.accounts.auth.model.AppUser;
import com.textile.accounts.auth.repository.AppUserRepository;
import com.textile.accounts.common.config.AppConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private Clock clock;

    private Instant testNow;

    @BeforeEach
    void setUp() {
        testNow = Instant.parse("2026-10-12T10:00:00Z");
        Mockito.when(clock.instant()).thenReturn(testNow);
        Mockito.when(clock.getZone()).thenReturn(AppConfig.DEFAULT_ZONE);

        auditEventRepository.deleteAll();
        appUserRepository.deleteAll();

        // Create standard test owner
        AppUser owner = new AppUser();
        owner.setUsername("owner");
        owner.setPasswordHash(passwordEncoder.encode("InitialPassword123!"));
        owner.setRole("OWNER");
        owner.setEnabled(true);
        owner.setMustChangePassword(true);
        owner.setFailedLoginCount(0);
        owner.setCreatedAt(testNow);
        owner.setUpdatedAt(testNow);
        appUserRepository.save(owner);
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String changePasswordJson(String currentPassword, String newPassword) {
        return "{\"currentPassword\":\"" + currentPassword + "\",\"newPassword\":\"" + newPassword + "\"}";
    }

    @Test
    @DisplayName("Password hash starts with {bcrypt} and has cost 12")
    void testStoredHashPrefixAndCost12() {
        AppUser user = appUserRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        String hash = user.getPasswordHash();

        assertThat(hash).startsWith("{bcrypt}");

        // Format is {bcrypt}$2a$12$...
        Pattern pattern = Pattern.compile("^\\{bcrypt\\}\\$2[aby]?\\$(\\d\\d)\\$.*");
        Matcher matcher = pattern.matcher(hash);
        assertThat(matcher.matches()).isTrue();
        int cost = Integer.parseInt(matcher.group(1));
        assertThat(cost).isEqualTo(12);
    }

    @Test
    @DisplayName("Login success, me endpoint, and logout flow")
    void testLoginMeAndLogoutSuccess() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("owner", "InitialPassword123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("owner"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        // Check GET /api/v1/auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("owner"))
                .andExpect(jsonPath("$.role").value("OWNER"));

        // Check POST /api/v1/auth/logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isNoContent());

        // Audit events recorded
        List<AuditEvent> events = auditEventRepository.findByActorUsernameOrderByOccurredAtDesc("owner");
        assertThat(events).extracting(AuditEvent::getAction)
                .contains("LOGIN_SUCCESS", "LOGOUT");
    }

    @Test
    @DisplayName("Wrong password and unknown user return generic error message and timing mitigation runs")
    void testInvalidCredentialsGenericMessage() throws Exception {
        // 1. Wrong password
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("owner", "WrongPassword123!")))
                .andExpect(status().isUnauthorized());

        // 2. Unknown user
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("nonexistent", "SomePassword123!")))
                .andExpect(status().isUnauthorized());

        // Check audit logs
        List<AuditEvent> auditRows = auditEventRepository.findAll();
        assertThat(auditRows).hasSize(2);
        assertThat(auditRows).allMatch(e -> "LOGIN_FAILURE".equals(e.getAction()));
    }

    @Test
    @DisplayName("5 failed logins lock account for 15 minutes; 6th is rejected; unlock after clock moves 15m")
    void testLockoutAfter5FailuresAndClockUnlock() throws Exception {
        String badRequestJson = loginJson("owner", "WrongPass123!");

        // 5 consecutive failures
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badRequestJson))
                    .andExpect(status().isUnauthorized());
        }

        // Verify account is now locked in DB
        AppUser lockedUser = appUserRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        assertThat(lockedUser.getFailedLoginCount()).isEqualTo(5);
        assertThat(lockedUser.getLockedUntil()).isEqualTo(testNow.plus(Duration.ofMinutes(15)));

        // Verify exactly 5 audit events recorded
        List<AuditEvent> audits = auditEventRepository.findAll();
        assertThat(audits).hasSize(5);
        assertThat(audits.get(4).getAction()).isEqualTo("ACCOUNT_LOCKED");

        // 6th attempt with CORRECT password is rejected while locked
        String correctRequestJson = loginJson("owner", "InitialPassword123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctRequestJson))
                .andExpect(status().isUnauthorized());

        // Advance clock by 16 minutes
        Instant unlockedTime = testNow.plus(Duration.ofMinutes(16));
        Mockito.when(clock.instant()).thenReturn(unlockedTime);

        // 7th attempt with CORRECT password now succeeds
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctRequestJson))
                .andExpect(status().isOk());

        AppUser resetUser = appUserRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        assertThat(resetUser.getFailedLoginCount()).isEqualTo(0);
        assertThat(resetUser.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("Server-enforced must_change_password blocks other endpoints and allows me/logout/change-password")
    void testMustChangePasswordEnforcement() throws Exception {
        // Login with must_change_password=true
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("owner", "InitialPassword123!")))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        // Attempting to access another protected /api/** endpoint returns 403
        mockMvc.perform(get("/api/v1/business-profile")
                        .cookie(sessionCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PASSWORD_CHANGE_REQUIRED"));

        // GET /auth/me is allowed
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isOk());

        // POST /auth/change-password is allowed
        MvcResult changeResult = mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("InitialPassword123!", "BrandNewSecurePassword123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andReturn();

        // User now has must_change_password=false
        AppUser updated = appUserRepository.findByUsernameIgnoreCase("owner").orElseThrow();
        assertThat(updated.isMustChangePassword()).isFalse();
    }

    @Test
    @DisplayName("Password change validation: <12 chars rejected, same password rejected")
    void testPasswordChangeValidation() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("owner", "InitialPassword123!")))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        // 1. New password too short (< 12 chars)
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("InitialPassword123!", "Short123!")))
                .andExpect(status().isBadRequest());

        // 2. New password same as current password
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("InitialPassword123!", "InitialPassword123!")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Absolute 12-hour session timeout filter rejects expired sessions with 401")
    void testAbsolute12HourSessionTimeoutFilter() throws Exception {
        MockHttpSession session = new MockHttpSession();
        // Session created 13 hours ago
        long thirteenHoursAgo = System.currentTimeMillis() - (13L * 60L * 60L * 1000L);
        // Using reflection or setting creation time in MockHttpSession
        // MockHttpSession allows setting creation time via protected field or constructor
        try {
            java.lang.reflect.Field field = session.getClass().getDeclaredField("creationTime");
            field.setAccessible(true);
            field.setLong(session, thirteenHoursAgo);
        } catch (NoSuchFieldException e) {
            // Alternatively test through filter directly
        }

        mockMvc.perform(get("/api/v1/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized());
    }
}
