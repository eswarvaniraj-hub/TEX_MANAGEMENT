package com.textile.accounts.auth;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.auth.model.AppUser;
import com.textile.accounts.auth.repository.AppUserRepository;
import com.textile.accounts.auth.service.OwnerBootstrapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OwnerBootstrapTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("Bootstrap creates owner when table is empty")
    void testBootstrapCreatesOwnerWhenEmpty() {
        when(appUserRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("{bcrypt}mocked_hash");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        OwnerBootstrapService service = new OwnerBootstrapService(appUserRepository, passwordEncoder, auditService, clock);
        ReflectionTestUtils.setField(service, "envOwnerUsername", "owner");
        ReflectionTestUtils.setField(service, "envOwnerPassword", "SecurePassword123!");

        service.run(new DefaultApplicationArguments());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("owner");
        assertThat(saved.getPasswordHash()).isEqualTo("{bcrypt}mocked_hash");
        assertThat(saved.getRole()).isEqualTo("OWNER");
        assertThat(saved.isMustChangePassword()).isTrue();
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getFailedLoginCount()).isEqualTo(0);

        verify(auditService).logSecurityEvent(eq("owner"), eq("OWNER_BOOTSTRAP"), anyString(), eq("127.0.0.1"), eq(1L));
    }

    @Test
    @DisplayName("Bootstrap skips creation when app_user table is not empty")
    void testBootstrapSkipsWhenNotEmpty() {
        when(appUserRepository.count()).thenReturn(1L);

        OwnerBootstrapService service = new OwnerBootstrapService(appUserRepository, passwordEncoder, auditService, clock);
        ReflectionTestUtils.setField(service, "envOwnerUsername", "owner");
        ReflectionTestUtils.setField(service, "envOwnerPassword", "SecurePassword123!");

        service.run(new DefaultApplicationArguments());

        verify(appUserRepository, never()).save(any());
        verify(auditService, never()).logSecurityEvent(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Bootstrap fails fast when password is under 12 characters")
    void testBootstrapFailsFastOnShortPassword() {
        when(appUserRepository.count()).thenReturn(0L);

        OwnerBootstrapService service = new OwnerBootstrapService(appUserRepository, passwordEncoder, auditService, clock);
        ReflectionTestUtils.setField(service, "envOwnerUsername", "owner");
        ReflectionTestUtils.setField(service, "envOwnerPassword", "ShortPass1!");

        assertThatThrownBy(() -> service.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("minimum 12 characters");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bootstrap fails fast when username is missing")
    void testBootstrapFailsFastOnMissingUsername() {
        when(appUserRepository.count()).thenReturn(0L);

        OwnerBootstrapService service = new OwnerBootstrapService(appUserRepository, passwordEncoder, auditService, clock);
        ReflectionTestUtils.setField(service, "envOwnerUsername", "");
        ReflectionTestUtils.setField(service, "envOwnerPassword", "ValidPassword123!");

        assertThatThrownBy(() -> service.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OWNER_USERNAME and OWNER_PASSWORD");

        verify(appUserRepository, never()).save(any());
    }
}
