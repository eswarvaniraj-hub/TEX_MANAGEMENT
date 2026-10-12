package com.textile.accounts.audit.dto;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        Instant occurredAt,
        Long actorUserId,
        String actorUsername,
        String action,
        String entityType,
        String entityId,
        String summary,
        String ipAddress
) {}
