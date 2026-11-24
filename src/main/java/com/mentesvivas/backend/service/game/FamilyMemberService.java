package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.dto.game.FamilyGalleryResponse;
import com.mentesvivas.backend.dto.game.FamilyMemberResponse;
import com.mentesvivas.backend.dto.game.RegisterFamilyMemberRequest;
import com.mentesvivas.backend.entity.game.FamilyMember;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.mapper.game.GameMapper;
import com.mentesvivas.backend.repository.game.FamilyMemberRepository;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FamilyMemberService {

    private final PatientCareAccess access;
    private final CaregiverPatientAccess caregiverAccess;
    private final FamilyMemberRepository repository;
    private final PhotoConsentGuard consent;
    private final RecognitionGamePolicy policy;
    private final Clock clock;

    @Transactional
    public FamilyMemberResponse register(UUID patientId, RegisterFamilyMemberRequest request) {
        User caregiver = caregiverAccess.requireRepresentativeOf(patientId);
        consent.requireConsent(patientId);

        FamilyMember member = new FamilyMember();
        member.setPatientId(patientId);
        member.setName(request.name().trim());
        member.setKinship(request.kinship().trim());
        member.setPhotoUrl(request.photoUrl().trim());
        member.setAudioUrl(blankToNull(request.audioUrl()));
        member.setCreatedBy(caregiver.getId());
        member.setCreatedAt(clock.instant());
        return GameMapper.toResponse(repository.save(member));
    }

    @Transactional(readOnly = true)
    public FamilyGalleryResponse gallery(UUID patientId) {
        access.requireWriter(patientId);
        List<FamilyMemberResponse> members = repository.findByPatientIdOrderByNameAsc(patientId).stream()
                .map(GameMapper::toResponse)
                .toList();
        return new FamilyGalleryResponse(members, policy.minimumPeople(), policy.isUnlocked(members.size()),
                policy.guidance(members.size()));
    }

    @Transactional
    public void remove(UUID patientId, UUID memberId) {
        caregiverAccess.requireRepresentativeOf(patientId);
        FamilyMember member = repository.findByIdAndPatientId(memberId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa nao encontrada."));
        repository.delete(member);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
