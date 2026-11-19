package com.mentesvivas.backend.service.terms;

import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.dto.terms.AcceptTermsRequest;
import com.mentesvivas.backend.dto.terms.TermsAcceptanceResponse;
import com.mentesvivas.backend.entity.terms.TermsAcceptance;
import com.mentesvivas.backend.entity.terms.TermsType;
import com.mentesvivas.backend.entity.terms.TermsVersion;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.terms.TermsNotPublishedException;
import com.mentesvivas.backend.exception.terms.TermsVersionChangedException;
import com.mentesvivas.backend.mapper.terms.TermsMapper;
import com.mentesvivas.backend.repository.terms.TermsAcceptanceRepository;
import com.mentesvivas.backend.repository.terms.TermsVersionRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsAcceptanceService {

    private final TermsService termsService;
    private final TermsVersionRepository termsVersionRepository;
    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final CurrentUser currentUser;
    private final AuditTrail auditTrail;
    private final CaregiverPatientAccess caregiverAccess;
    private final UserRepository userRepository;

    public List<Integer> validateSignup(Role role, Map<TermsType, Integer> informedVersions) {
        List<Integer> versions = new ArrayList<>();
        for (TermsType type : TermsType.values()) {
            if (!termsService.hasAny(type)) {
                if (type == TermsType.TERMS_OF_USE) {
                    throw new TermsNotPublishedException();
                }
                continue;
            }
            Optional<TermsVersion> current = termsService.currentFor(type, role);
            if (current.isEmpty()) {
                continue;
            }
            if (!current.get().getVersion().equals(informedVersions.get(type))) {
                throw new TermsVersionChangedException();
            }
            versions.add(current.get().getVersion());
        }
        return versions;
    }

    @Transactional
    public void registerAll(User user, List<Integer> versions) {
        versions.forEach(version -> register(user, version));
    }

    @Transactional
    public TermsAcceptanceResponse accept(AcceptTermsRequest request) {
        return acceptFor(currentUser.get(), request, null);
    }

    @Transactional
    public TermsAcceptanceResponse acceptOnBehalfOf(UUID patientId, AcceptTermsRequest request) {
        User representative = caregiverAccess.requireRepresentativeOf(patientId);
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente nao encontrado."));
        return acceptFor(patient, request, representative.getId());
    }

    private TermsAcceptanceResponse acceptFor(User user, AcceptTermsRequest request, UUID representativeId) {
        TermsVersion terms = termsVersionRepository.findById(request.version())
                .orElseThrow(TermsVersionChangedException::new);
        if (!terms.appliesTo(user.getRole())) {
            throw new ConflictException("Este documento nao se aplica ao seu perfil.");
        }
        boolean isCurrent = termsService.currentFor(terms.getType(), user.getRole())
                .map(current -> current.getVersion().equals(terms.getVersion()))
                .orElse(false);
        if (!isCurrent) {
            throw new TermsVersionChangedException();
        }
        return TermsMapper.toResponse(register(user, terms.getVersion(), representativeId));
    }

    public List<TermsAcceptanceResponse> currentUserAcceptances() {
        return termsAcceptanceRepository.findByUserIdOrderByVersionAsc(currentUser.get().getId()).stream()
                .map(TermsMapper::toResponse)
                .toList();
    }

    public boolean acceptedCurrentVersion(User user) {
        if (user.getRole() == Role.ADMINISTRATOR) {
            return true;
        }
        return termsService.applicableCurrent(user.getRole()).stream()
                .allMatch(terms -> termsAcceptanceRepository.existsByUserIdAndVersion(user.getId(), terms.getVersion()));
    }

    private TermsAcceptance register(User user, Integer version) {
        return register(user, version, null);
    }

    private TermsAcceptance register(User user, Integer version, UUID representativeId) {
        return termsAcceptanceRepository.findByUserIdAndVersion(user.getId(), version)
                .orElseGet(() -> {
                    TermsAcceptance saved = termsAcceptanceRepository.save(new TermsAcceptance(user, version, Instant.now()));
                    AuditRecord record = AuditRecord.of(AuditEventType.TERMS_ACCEPTED,
                            representativeId == null ? user.getId() : representativeId).withValues(null, "v" + version);
                    auditTrail.record(representativeId == null ? record : record.forPatient(user.getId()).onBehalfOf(user.getId()));
                    return saved;
                });
    }
}
