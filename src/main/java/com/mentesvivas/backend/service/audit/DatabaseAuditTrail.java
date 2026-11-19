package com.mentesvivas.backend.service.audit;

import com.mentesvivas.backend.entity.audit.AuditEvent;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class DatabaseAuditTrail implements AuditTrail {

    private final AuditEventRepository repository;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(AuditRecord record) {
        repository.save(new AuditEvent(clock.instant(), record));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependently(AuditRecord record) {
        repository.save(new AuditEvent(clock.instant(), record));
    }
}
