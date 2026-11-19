package com.mentesvivas.backend.service.audit;

import com.mentesvivas.backend.entity.audit.AccessEvent;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.repository.audit.AccessEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DatabaseAccessLog implements AccessLog {

    private final AccessEventRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    public void record(UUID accessorUserId, UUID patientUserId, PatientDataArea area) {
        if (accessorUserId.equals(patientUserId)) {
            return;
        }
        repository.save(new AccessEvent(clock.instant(), accessorUserId, patientUserId, area));
    }
}
