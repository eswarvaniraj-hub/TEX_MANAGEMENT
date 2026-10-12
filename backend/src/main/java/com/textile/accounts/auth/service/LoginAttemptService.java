package com.textile.accounts.auth.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.auth.model.AppUser;
import com.textile.accounts.auth.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Service
public class LoginAttemptService {

    private final AppUserRepository appUserRepository;
    private final AuditService auditService;
    private final Clock clock;

    public LoginAttemptService(AppUserRepository appUserRepository, AuditService auditService, Clock clock) {
        this.appUserRepository = appUserRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Commits failed login attempt counter, lockout timestamp, and audit event
     * in an independent transaction (REQUIRES_NEW) so they persist even when the
     * login request throws an exception or rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(Long userId, String username, String ipAddress) {
        AppUser user = appUserRepository.findById(userId).orElse(null);
        if (user == null) {
            auditService.logSecurityEvent(username, "LOGIN_FAILURE", "Login failed: invalid username or password", ipAddress, null);
            return;
        }

        int newCount = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(newCount);
        user.setUpdatedAt(clock.instant());

        if (newCount >= 5) {
            user.setLockedUntil(clock.instant().plus(Duration.ofMinutes(15)));
            appUserRepository.save(user);
            auditService.logSecurityEvent(user.getUsername(), "ACCOUNT_LOCKED", "Account locked for 15 minutes after 5 consecutive failed login attempts", ipAddress, user.getId());
        } else {
            appUserRepository.save(user);
            auditService.logSecurityEvent(user.getUsername(), "LOGIN_FAILURE", "Login failed: invalid username or password (attempt " + newCount + " of 5)", ipAddress, user.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordBlockedLockedAttempt(Long userId, String username, String ipAddress) {
        auditService.logSecurityEvent(username, "LOGIN_BLOCKED", "Login blocked: account is temporarily locked", ipAddress, userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUnknownUserAttempt(String username, String ipAddress) {
        auditService.logSecurityEvent(username, "LOGIN_FAILURE", "Login failed: invalid username or password", ipAddress, null);
    }
}
