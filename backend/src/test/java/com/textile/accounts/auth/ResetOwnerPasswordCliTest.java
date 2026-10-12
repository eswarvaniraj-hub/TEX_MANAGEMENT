package com.textile.accounts.auth;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.auth.model.AppUser;
import com.textile.accounts.auth.repository.AppUserRepository;
import com.textile.accounts.common.cli.ResetOwnerPasswordRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResetOwnerPasswordCliTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("CLI runner successfully resets owner password, unlocks account, and sets must_change_password=true")
    void testResetOwnerPasswordSuccess() {
        AppUser owner = new AppUser();
        owner.setId(10L);
        owner.setUsername("owner");
        owner.setRole("OWNER");
        owner.setFailedLoginCount(5);
        owner.setLockedUntil(Instant.now().plusSeconds(900));
        owner.setMustChangePassword(false);

        when(appUserRepository.findAll()).thenReturn(List.of(owner));
        when(passwordEncoder.encode("NewSecretPassword123!")).thenReturn("{bcrypt}new_cli_hash");

        ResetOwnerPasswordRunner runner = new ResetOwnerPasswordRunner(
                appUserRepository,
                passwordEncoder,
                auditService,
                clock
        );

        runner.run("NewSecretPassword123!");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();

        assertThat(saved.getPasswordHash()).isEqualTo("{bcrypt}new_cli_hash");
        assertThat(saved.getFailedLoginCount()).isEqualTo(0);
        assertThat(saved.getLockedUntil()).isNull();
        assertThat(saved.isMustChangePassword()).isTrue();
        assertThat(saved.getUpdatedAt()).isEqualTo(clock.instant());

        verify(auditService).logSecurityEvent(
                eq("owner"),
                eq("OWNER_PASSWORD_RESET_CLI"),
                anyString(),
                eq("127.0.0.1"),
                eq(10L)
        );
    }

    @Test
    @DisplayName("CLI runner rejects passwords under 12 characters and aborts without saving")
    void testResetOwnerPasswordShortPassword() {
        ResetOwnerPasswordRunner runner = new ResetOwnerPasswordRunner(
                appUserRepository,
                passwordEncoder,
                auditService,
                clock
        );

        runner.run("Short123!");

        verify(appUserRepository, never()).findAll();
        verify(appUserRepository, never()).save(any());
        verify(auditService, never()).logSecurityEvent(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("CLI runner handles missing owner account safely")
    void testResetOwnerPasswordNoOwnerFound() {
        when(appUserRepository.findAll()).thenReturn(Collections.emptyList());

        ResetOwnerPasswordRunner runner = new ResetOwnerPasswordRunner(
                appUserRepository,
                passwordEncoder,
                auditService,
                clock
        );

        runner.run("ValidLongNewPassword123!");

        verify(appUserRepository).findAll();
        verify(appUserRepository, never()).save(any());
        verify(auditService, never()).logSecurityEvent(any(), any(), any(), any(), any());
    }
}
