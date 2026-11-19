package com.mentesvivas.backend.service.audit;

import com.mentesvivas.backend.entity.audit.PatientDataArea;

import java.util.UUID;

public interface AccessLog {

    void record(UUID accessorUserId, UUID patientUserId, PatientDataArea area);
}
