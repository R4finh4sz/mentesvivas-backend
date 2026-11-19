package com.mentesvivas.backend.repository.audit;

import com.mentesvivas.backend.entity.audit.AccessEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.UUID;

public interface AccessEventRepository extends Repository<AccessEvent, UUID> {

    AccessEvent save(AccessEvent event);

    Page<AccessEvent> findByPatientUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            UUID patientUserId, Instant from, Instant to, Pageable pageable);

    long countByPatientUserId(UUID patientUserId);
}
