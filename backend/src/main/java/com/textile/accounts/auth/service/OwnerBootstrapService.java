package com.textile.accounts.auth.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.auth.model.AppUser;
import com.textile.accounts.auth.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Profile("!reset-owner-password")
public class OwnerBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerBootstrapService.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    @Value("${OWNER_USERNAME:#{null}}")
    private String envOwnerUsername;

    @Value("${OWNER_PASSWORD:#{null}}")
    private String envOwnerPassword;

    public OwnerBootstrapService(
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
    @Transactional
    public void run(ApplicationArguments args) {
        if (appUserRepository.count() > 0) {
            log.info("AppUser table already contains users. Skipping owner bootstrap.");
            return;
        }

        String username = envOwnerUsername != null ? envOwnerUsername.trim() : null;
        String password = envOwnerPassword != null ? envOwnerPassword.trim() : null;

        if (username == null || username.isBlank() || password == null || password.length() < 12) {
            throw new IllegalStateException(
                "Initial owner bootstrap failed: OWNER_USERNAME and OWNER_PASSWORD (minimum 12 characters) " +
                "must be configured in environment variables when the app_user table is empty."
            );
        }

        AppUser owner = new AppUser();
        owner.setUsername(username);
        owner.setPasswordHash(passwordEncoder.encode(password));
        owner.setRole("OWNER");
        owner.setEnabled(true);
        owner.setMustChangePassword(true);
        Instant now = (clock != null && clock.instant() != null) ? clock.instant() : Instant.now();
        owner.setCreatedAt(now);
        owner.setUpdatedAt(now);

        AppUser saved = appUserRepository.save(owner);
        auditService.logSecurityEvent(
            saved.getUsername(),
            "OWNER_BOOTSTRAP",
            "Initial OWNER account bootstrapped from environment configuration with must_change_password=true",
            "127.0.0.1",
            saved.getId()
        );

        log.info("Successfully bootstrapped initial OWNER account '{}' with must_change_password=true.", username);
    }
}
