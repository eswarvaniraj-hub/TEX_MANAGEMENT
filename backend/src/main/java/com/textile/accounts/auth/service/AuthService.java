package com.textile.accounts.auth.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.auth.dto.ChangePasswordRequest;
import com.textile.accounts.auth.dto.LoginRequest;
import com.textile.accounts.auth.dto.UserProfileResponse;
import com.textile.accounts.auth.model.AppUser;
import com.textile.accounts.auth.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    public static final String GENERIC_AUTH_ERROR = "Invalid username or password";
    // Pre-computed BCrypt cost 12 dummy hash to defend against username timing enumeration
    private static final String DUMMY_BCRYPT_HASH = "{bcrypt}$2a$12$K8pQ0e0x0e0x0e0x0e0x0e0x0e0x0e0x0e0x0e0x0e0x0e0x0e0x0";

    private final AppUserRepository appUserRepository;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;
    private final Clock clock;

    public AuthService(
            AppUserRepository appUserRepository,
            AuditService auditService,
            LoginAttemptService loginAttemptService,
            PasswordEncoder passwordEncoder,
            SecurityContextRepository securityContextRepository,
            Clock clock
    ) {
        this.appUserRepository = appUserRepository;
        this.auditService = auditService;
        this.loginAttemptService = loginAttemptService;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
        this.clock = clock;
    }

    @Transactional
    public UserProfileResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String ipAddress = extractClientIp(httpRequest);
        String username = request.username().trim();
        Optional<AppUser> userOpt = appUserRepository.findByUsernameIgnoreCase(username);

        if (userOpt.isEmpty()) {
            // Timing mitigation: perform dummy BCrypt computation
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            loginAttemptService.recordUnknownUserAttempt(username, ipAddress);
            throw new BadCredentialsException(GENERIC_AUTH_ERROR);
        }

        AppUser user = userOpt.get();

        // 1. Check account lockout
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(clock.instant())) {
            loginAttemptService.recordBlockedLockedAttempt(user.getId(), user.getUsername(), ipAddress);
            throw new LockedException(GENERIC_AUTH_ERROR);
        }

        // If a past lockout has expired, reset counter
        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(clock.instant())) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
            appUserRepository.save(user);
        }

        // 2. Validate password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailedAttempt(user.getId(), user.getUsername(), ipAddress);
            throw new BadCredentialsException(GENERIC_AUTH_ERROR);
        }

        // 3. Login success
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(clock.instant());
        user.setUpdatedAt(clock.instant());
        appUserRepository.save(user);

        auditService.logSecurityEvent(user.getUsername(), "LOGIN_SUCCESS", "User login successful", ipAddress, user.getId());

        // 4. Session management & fixation protection
        establishSessionAuthentication(user, httpRequest, httpResponse);

        return new UserProfileResponse(user.getUsername(), user.getRole(), user.isMustChangePassword());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String username) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return new UserProfileResponse(user.getUsername(), user.getRole(), user.isMustChangePassword());
    }

    @Transactional
    public UserProfileResponse changePassword(
            ChangePasswordRequest request,
            String currentUsername,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ipAddress = extractClientIp(httpRequest);
        AppUser user = appUserRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            auditService.logSecurityEvent(user.getUsername(), "PASSWORD_CHANGE_FAILED", "Failed password change: current password incorrect", ipAddress, user.getId());
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Verify minimum length
        if (request.newPassword() == null || request.newPassword().length() < 12) {
            throw new IllegalArgumentException("New password must be at least 12 characters");
        }

        // Verify new password differs from old password
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Update password and clear must_change_password flag
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setUpdatedAt(clock.instant());
        appUserRepository.save(user);

        auditService.logSecurityEvent(user.getUsername(), "PASSWORD_CHANGE_SUCCESS", "User password changed successfully", ipAddress, user.getId());

        // Requirement 9: Invalidate old session and start a new session
        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        establishSessionAuthentication(user, httpRequest, httpResponse);

        return new UserProfileResponse(user.getUsername(), user.getRole(), false);
    }

    public void logout(String currentUsername, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String ipAddress = extractClientIp(httpRequest);
        if (currentUsername != null) {
            auditService.logSecurityEvent(currentUsername, "LOGOUT", "User logged out successfully", ipAddress, null);
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private void establishSessionAuthentication(AppUser user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // Change session ID if existing, or create new session (session fixation protection)
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            httpRequest.changeSessionId();
        } else {
            httpRequest.getSession(true);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
