package com.textile.accounts.common.cli;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.auth.model.AppUser;
import com.textile.accounts.auth.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
@Profile("reset-owner-password")
public class ResetOwnerPasswordRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ResetOwnerPasswordRunner.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    public ResetOwnerPasswordRunner(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            Clock clock
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        log.info("Running offline reset-owner-password CLI routine...");

        String newPassword = (args != null && args.length > 0 && !args[0].isBlank())
                ? args[0].trim()
                : System.getenv("NEW_OWNER_PASSWORD");
        if (newPassword == null || newPassword.isBlank()) {
            if (System.console() != null) {
                char[] chars = System.console().readPassword("Enter new owner password (min 12 chars): ");
                if (chars != null) {
                    newPassword = new String(chars);
                }
            }
        }

        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 12) {
            log.error("Reset failed: NEW_OWNER_PASSWORD environment variable is missing or under 12 characters.");
            System.err.println("ERROR: NEW_OWNER_PASSWORD must be configured with at least 12 characters.");
            return;
        }

        List<AppUser> users = appUserRepository.findAll();
        AppUser owner = users.stream()
                .filter(u -> "OWNER".equalsIgnoreCase(u.getRole()))
                .findFirst()
                .orElse(null);

        if (owner == null) {
            log.error("Reset failed: No user with role OWNER found in the database.");
            System.err.println("ERROR: No OWNER account found in database.");
            return;
        }

        owner.setPasswordHash(passwordEncoder.encode(newPassword));
        owner.setFailedLoginCount(0);
        owner.setLockedUntil(null);
        owner.setMustChangePassword(true);
        java.time.Instant now = (clock != null && clock.instant() != null) ? clock.instant() : java.time.Instant.now();
        owner.setUpdatedAt(now);
        appUserRepository.save(owner);

        auditService.logSecurityEvent(
                owner.getUsername(),
                "OWNER_PASSWORD_RESET_CLI",
                "Owner password was reset via offline CLI runner with must_change_password=true and account unlocked",
                "127.0.0.1",
                owner.getId()
        );

        log.info("Owner password for '{}' has been successfully reset.", owner.getUsername());
        System.out.println("SUCCESS: Owner password has been reset. must_change_password set to true. Account unlocked.");
    }
}
