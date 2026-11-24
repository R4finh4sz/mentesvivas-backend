package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.dto.clinical.DeclareDiagnosisRequest;
import com.mentesvivas.backend.dto.clinical.DiagnosisResponse;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.patient.Cid10Entry;
import com.mentesvivas.backend.entity.patient.DiagnosisRevision;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.patient.Cid10Repository;
import com.mentesvivas.backend.repository.patient.DiagnosisRevisionRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.audit.AuditTrail;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private static final Pattern CODE = Pattern.compile("[A-Z][0-9]{2}(\\.?[0-9])?");
    private static final Pattern PREFIX = Pattern.compile("[A-Z][0-9]{0,3}|[A-Z][0-9]{2}\\.[0-9]?");

    private final Cid10Repository catalog;
    private final DiagnosisRevisionRepository revisions;
    private final DoctorPatientAccess doctors;
    private final CurrentUser currentUser;
    private final AuditTrail audit;
    private final Clock clock;

    @Transactional
    public int importCatalog(List<CatalogInput> entries) {
        var codes = new HashSet<String>();
        List<Cid10Entry> normalized = entries.stream().map(entry -> {
            String code = normalizeCode(entry.code());
            if (!codes.add(code)) {
                throw new ImpossibleValueException("Código duplicado no lote: " + code);
            }
            return new Cid10Entry(code, entry.description().trim());
        }).toList();
        catalog.saveAll(normalized);
        audit.record(AuditRecord.of(AuditEventType.CID10_CATALOG_UPDATED, currentUser.get().getId())
                .withValues(null, Integer.toString(normalized.size())));
        return normalized.size();
    }

    @Transactional(readOnly = true)
    public List<CatalogInput> search(String rawPrefix) {
        doctors.requireDoctor();
        String prefix = rawPrefix.trim().toUpperCase(Locale.ROOT);
        if (!PREFIX.matcher(prefix).matches()) {
            throw new ImpossibleValueException("Informe um código CID-10 ou o início do código.");
        }
        return catalog.findByCodeStartingWithOrderByCodeAsc(dot(prefix), PageRequest.of(0, 50)).stream()
                .map(entry -> new CatalogInput(entry.getCode(), entry.getDescription())).toList();
    }

    public DiagnosisResponse coded(UUID patientId, UUID doctorId, DeclareDiagnosisRequest request) {
        String primary = normalizeCode(request.primaryCid());
        List<String> secondary = request.secondaryCids() == null ? List.of()
                : request.secondaryCids().stream().map(DiagnosisService::normalizeCode).toList();
        var requiredCodes = new HashSet<String>(secondary);
        if (requiredCodes.size() != secondary.size() || !requiredCodes.add(primary)) {
            throw new ImpossibleValueException("O CID principal e os secundários precisam ser distintos.");
        }
        if (catalog.findAllById(requiredCodes).size() != requiredCodes.size()) {
            throw new ImpossibleValueException("Um ou mais códigos não constam do catálogo CID-10.");
        }
        return append(patientId, doctorId, request.disease().trim(), primary, secondary);
    }

    public void legacy(UUID patientId, UUID doctorId, String disease) {
        boolean unchanged = revisions.findFirstByPatientIdOrderByRecordedAtDescIdDesc(patientId)
                .filter(previous -> previous.getDisease().equals(disease)).isPresent();
        if (!unchanged) {
            append(patientId, doctorId, disease, null, List.of());
        }
    }

    public List<DiagnosisResponse> history(UUID patientId, int page, int size) {
        return revisions.findByPatientIdOrderByRecordedAtDescIdDesc(patientId, PageRequest.of(page, size)).stream()
                .map(DiagnosisService::view).toList();
    }

    private DiagnosisResponse append(UUID patientId, UUID doctorId, String disease, String primary,
                                      List<String> secondary) {
        DiagnosisRevision revision = revisions.save(
                new DiagnosisRevision(patientId, doctorId, clock.instant(), disease, primary, secondary));
        audit.record(AuditRecord.of(AuditEventType.DIAGNOSIS_REVISION_RECORDED, doctorId).forPatient(patientId)
                .withValues(null, revision.getId().toString()));
        return view(revision);
    }

    private static DiagnosisResponse view(DiagnosisRevision revision) {
        return new DiagnosisResponse(revision.getId(), revision.getDisease(), revision.getPrimaryCid(),
                List.copyOf(revision.getSecondaryCids()), revision.getRecordedBy(), revision.getRecordedAt());
    }

    private static String normalizeCode(String raw) {
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new ImpossibleValueException("Formato de código CID-10 inválido.");
        }
        return dot(code);
    }

    private static String dot(String code) {
        return code.length() == 4 && code.charAt(3) != '.' ? code.substring(0, 3) + "." + code.charAt(3) : code;
    }

    public record CatalogInput(String code, String description) {}
}
