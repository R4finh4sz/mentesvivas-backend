package com.mentesvivas.backend.service.audit;

import com.mentesvivas.backend.entity.audit.AuditRecord;

public interface AuditTrail {

    void record(AuditRecord record);

    void recordIndependently(AuditRecord record);
}
