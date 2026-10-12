package com.textile.accounts.audit.service;

import com.textile.accounts.audit.model.AuditEvent;
import com.textile.accounts.audit.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final Clock clock;

    public AuditService(AuditEventRepository auditEventRepository, Clock clock) {
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
    }

    /**
     * Writes an audit event in an independent transaction (REQUIRES_NEW),
     * ensuring audit rows persist even if the calling request subsequently fails or rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent logEvent(
            Long actorUserId,
            String actorUsername,
            String action,
            String entityType,
            String entityId,
            String summary,
            String details,
            String ipAddress
    ) {
        AuditEvent event = new AuditEvent();
        Instant occurredAt = (clock != null && clock.instant() != null) ? clock.instant() : Instant.now();
        event.setOccurredAt(occurredAt);
        event.setActorUserId(actorUserId);
        event.setActorUsername(actorUsername != null ? actorUsername : "ANONYMOUS");
        event.setAction(action);
        event.setEntityType(entityType != null ? entityType : "SECURITY");
        event.setEntityId(entityId);
        event.setSummary(summary);
        event.setDetails(details);
        event.setIpAddress(ipAddress);

        return auditEventRepository.save(event);
    }

    /**
     * Helper to log authentication and security events. Never logs passwords or secret tokens.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent logSecurityEvent(String username, String action, String summary, String ipAddress, Long actorUserId) {
        return logEvent(actorUserId, username, action, "SECURITY", null, summary, null, ipAddress);
    }
}
