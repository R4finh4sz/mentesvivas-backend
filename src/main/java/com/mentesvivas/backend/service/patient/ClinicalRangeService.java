package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.ClinicalRangePolicyResponse;
import com.mentesvivas.backend.dto.patient.PublishClinicalRangesRequest;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.patient.ClinicalColor;
import com.mentesvivas.backend.entity.patient.ClinicalMetric;
import com.mentesvivas.backend.entity.patient.ClinicalRange;
import com.mentesvivas.backend.entity.patient.ClinicalRangePolicy;
import com.mentesvivas.backend.entity.patient.VitalSign;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.patient.ClinicalRangePolicyRepository;
import com.mentesvivas.backend.service.alert.CriticalAlertService;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.clinical.DoctorPatientAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClinicalRangeService {

    private static final String NOTICE = "A classificação apoia a decisão clínica e não é um diagnóstico.";

    private final ClinicalRangePolicyRepository repository;
    private final DoctorPatientAccess doctors;
    private final CriticalAlertService alerts;
    private final AuditTrail audit;
    private final Clock clock;

    @Transactional
    public ClinicalRangePolicyResponse publish(PublishClinicalRangesRequest request) {
        UUID doctorId = doctors.requireDoctor().getId();
        List<ClinicalRange> ranges = request.ranges().stream()
                .map(band -> new ClinicalRange(band.metric(), band.lowerBound(), band.color())).toList();
        validate(ranges);
        ClinicalRangePolicy policy = repository.save(new ClinicalRangePolicy(doctorId, clock.instant(), ranges));
        audit.record(AuditRecord.of(AuditEventType.CLINICAL_RANGES_PUBLISHED, doctorId)
                .withValues(null, policy.getId().toString()));
        return response(policy);
    }

    @Transactional(readOnly = true)
    public ClinicalRangePolicyResponse active() {
        return repository.findFirstByOrderByApprovedAtDescIdDesc().map(ClinicalRangeService::response)
                .orElseThrow(() -> new ResourceNotFoundException("Faixas clínicas ainda não publicadas."));
    }

    public void classify(VitalSign sign) {
        repository.findFirstByOrderByApprovedAtDescIdDesc().ifPresent(policy -> {
            var classified = new EnumMap<ClinicalMetric, ClinicalColor>(ClinicalMetric.class);
            for (ClinicalRange range : policy.getRanges()) {
                BigDecimal value = range.metric().valueOf(sign);
                if (value != null && (range.lowerBound() == null || value.compareTo(range.lowerBound()) >= 0)) {
                    classified.put(range.metric(), range.color());
                }
            }
            classified.values().stream().max(Comparator.naturalOrder()).ifPresent(color -> {
                sign.setClinicalColor(color);
                sign.setClinicalPolicyId(policy.getId());
            });
        });
    }

    public void notifyIfCritical(VitalSign sign) {
        if (sign.getClinicalColor() == ClinicalColor.RED) {
            alerts.create(sign.getPatientId(), sign.getRecordedBy(), NotificationType.CRITICAL_MEASUREMENT,
                    sign.getId(), "Uma aferição recebeu alerta clínico. Consulte o registro do paciente.");
        }
    }

    private void validate(List<ClinicalRange> ranges) {
        var byMetric = ranges.stream().collect(Collectors.groupingBy(ClinicalRange::metric));
        for (ClinicalMetric metric : ClinicalMetric.values()) {
            List<ClinicalRange> bands = byMetric.get(metric);
            if (bands == null || bands.getFirst().lowerBound() != null) {
                throw new ImpossibleValueException("A primeira faixa de " + metric + " precisa ter limite aberto.");
            }
            BigDecimal previous = null;
            for (ClinicalRange band : bands.subList(1, bands.size())) {
                BigDecimal bound = band.lowerBound();
                if (bound == null || (previous != null && bound.compareTo(previous) <= 0)) {
                    throw new ImpossibleValueException("Os limites de " + metric + " precisam ser crescentes.");
                }
                previous = bound;
            }
        }
    }

    private static ClinicalRangePolicyResponse response(ClinicalRangePolicy policy) {
        return new ClinicalRangePolicyResponse(policy.getId(), policy.getApprovedAt(),
                List.copyOf(policy.getRanges()), NOTICE);
    }
}
