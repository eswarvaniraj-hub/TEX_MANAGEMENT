package com.textile.accounts.audit.controller;

import com.textile.accounts.audit.dto.AuditEventResponse;
import com.textile.accounts.audit.model.AuditEvent;
import com.textile.accounts.audit.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AuditEventResponse>> getRecentEvents(
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        List<AuditEvent> events = auditService.getRecentEvents(limit);
        List<AuditEventResponse> responses = events.stream()
                .map(e -> new AuditEventResponse(
                        e.getId(),
                        e.getOccurredAt(),
                        e.getActorUserId(),
                        e.getActorUsername(),
                        e.getAction(),
                        e.getEntityType(),
                        e.getEntityId(),
                        e.getSummary(),
                        e.getIpAddress()
                ))
                .toList();
        return ResponseEntity.ok(responses);
    }
}
