package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.config.DoseProperties;
import com.mentesvivas.backend.dto.medication.CorrectDoseRequest;
import com.mentesvivas.backend.dto.medication.DoseResponse;
import com.mentesvivas.backend.dto.medication.RecordDoseRequest;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.medication.DoseCorrection;
import com.mentesvivas.backend.entity.medication.DoseRecord;
import com.mentesvivas.backend.entity.medication.DoseStatus;
import com.mentesvivas.backend.entity.medication.Medication;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.medication.DoseCorrectionRepository;
import com.mentesvivas.backend.repository.medication.DoseRecordRepository;
import com.mentesvivas.backend.repository.medication.MedicationRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoseService {

    private final PatientCareAccess access;
    private final MedicationRepository medicationRepository;
    private final DoseRecordRepository repository;
    private final DoseCorrectionRepository correctionRepository;
    private final DoseStockEffect stockEffect;
    private final DoseProperties properties;
    private final AccessLog accessLog;
    private final Clock clock;

    @Transactional
    public DoseResponse record(UUID patientId, UUID medicationId, RecordDoseRequest request) {
        User author = access.requireWriter(patientId);
        Medication medication = lock(patientId, medicationId);
        if (!medication.isActive()) {
            throw new ConflictException("Este medicamento esta inativo.");
        }
        Instant now = clock.instant();
        Optional<DoseRecord> original = repository
                .findFirstByMedicationIdAndScheduledForAndDuplicateFalse(medicationId, request.scheduledFor());
        DoseRecord dose = newDose(patientId, medicationId, request, author, now);
        if (original.isPresent()) {
            rejectDoubleClick(original.get(), author, now);
            dose.setDuplicate(true);
        } else if (request.status() == DoseStatus.TAKEN) {
            dose.setStockDiscounted(stockEffect.discount(medication));
        }
        return DoseResponse.from(repository.save(dose));
    }

    @Transactional
    public DoseResponse correct(UUID patientId, UUID doseId, CorrectDoseRequest request) {
        User author = access.requireWriter(patientId);
        DoseRecord dose = repository.findByIdAndPatientId(doseId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Dose nao encontrada."));
        if (dose.isDuplicate()) {
            throw new ConflictException("Uma dose duplicada nao pode ser corrigida.");
        }
        if (dose.getStatus() == request.status()) {
            throw new ConflictException("A dose ja esta com este status.");
        }
        Medication medication = lock(patientId, dose.getMedicationId());
        correctionRepository.save(new DoseCorrection(dose.getId(), dose.getStatus(), request.status(),
                request.justification().strip(), author.getId(), clock.instant()));
        stockEffect.apply(medication, dose, request.status());
        dose.setStatus(request.status());
        dose.setCorrected(true);
        return DoseResponse.from(dose);
    }

    @Transactional
    public List<DoseResponse> list(UUID patientId, Instant from, Instant to) {
        User reader = access.requireReader(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.MEDICATIONS);
        return repository.findByPatientIdAndScheduledForBetweenOrderByScheduledForDesc(patientId, from, to)
                .stream().map(DoseResponse::from).toList();
    }

    private Medication lock(UUID patientId, UUID medicationId) {
        return medicationRepository.lockByIdAndPatientId(medicationId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento nao encontrado."));
    }

    private DoseRecord newDose(UUID patientId, UUID medicationId, RecordDoseRequest request, User author,
                               Instant now) {
        DoseRecord dose = new DoseRecord();
        dose.setMedicationId(medicationId);
        dose.setPatientId(patientId);
        dose.setScheduledFor(request.scheduledFor());
        dose.setStatus(request.status());
        dose.setStockDiscounted(BigDecimal.ZERO);
        dose.setRecordedBy(author.getId());
        dose.setRecordedAt(now);
        return dose;
    }

    private void rejectDoubleClick(DoseRecord original, User author, Instant now) {
        boolean sameAuthor = original.getRecordedBy().equals(author.getId());
        boolean withinWindow = Duration.between(original.getRecordedAt(), now)
                .compareTo(Duration.ofSeconds(properties.doubleClickSeconds())) < 0;
        if (sameAuthor && withinWindow) {
            throw new ConflictException("Esta dose acabou de ser confirmada. Aguarde alguns segundos.");
        }
    }
}
