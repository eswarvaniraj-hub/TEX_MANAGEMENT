package com.textile.accounts.audit.repository;

import com.textile.accounts.audit.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByActorUsernameOrderByOccurredAtDesc(String actorUsername);

    List<AuditEvent> findByActionOrderByOccurredAtDesc(String action);
}
