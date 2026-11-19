package com.mentesvivas.backend.repository.audit;

import com.mentesvivas.backend.entity.audit.AuditEvent;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends Repository<AuditEvent, UUID> {

    AuditEvent save(AuditEvent event);

    Optional<AuditEvent> findById(UUID id);

    List<AuditEvent> findByPatientUserIdOrderByOccurredAtDesc(UUID patientUserId);

    List<AuditEvent> findByActorUserIdOrderByOccurredAtDesc(UUID actorUserId);

    boolean existsByCorrectsEventId(UUID correctsEventId);
}
