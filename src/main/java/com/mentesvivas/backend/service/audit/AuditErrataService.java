package com.mentesvivas.backend.service.audit;

import com.mentesvivas.backend.dto.audit.ErrataResponse;
import com.mentesvivas.backend.entity.audit.AuditEvent;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditErrataService {

    private final AuditEventRepository repository;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Transactional
    public ErrataResponse correct(UUID eventId, String justification) {
        AuditEvent original = repository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento de auditoria nao encontrado."));
        if (original.getEventType() == AuditEventType.ERRATA) {
            throw new ConflictException("Uma errata nao pode ser corrigida.");
        }
        if (repository.existsByCorrectsEventId(eventId)) {
            throw new ConflictException("Este evento ja possui errata.");
        }
        AuditEvent errata = repository.save(new AuditEvent(clock.instant(),
                AuditRecord.errata(currentUser.get().getId(), original.getId(), original.getPatientUserId(), justification)));
        return new ErrataResponse(errata.getId(), eventId, errata.getOccurredAt());
    }
}
