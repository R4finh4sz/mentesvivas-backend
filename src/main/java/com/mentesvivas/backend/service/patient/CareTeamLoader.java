package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.CareTeamResponse;
import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.DoctorPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.mapper.link.LinkMapper;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.repository.link.DoctorPatientLinkRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CareTeamLoader {

    private final DoctorPatientLinkRepository doctorLinkRepository;
    private final CaregiverPatientLinkRepository caregiverLinkRepository;
    private final UserRepository userRepository;

    public CareTeamResponse load(UUID patientId) {
        List<UUID> doctorIds = doctorLinkRepository.findByPatientIdAndStatus(patientId, LinkStatus.ACTIVE).stream()
                .map(DoctorPatientLink::getDoctorId)
                .toList();
        List<UUID> caregiverIds = caregiverLinkRepository.findByPatientIdAndStatus(patientId, LinkStatus.ACTIVE).stream()
                .map(CaregiverPatientLink::getCaregiverId)
                .toList();
        return new CareTeamResponse(
                userRepository.findAllById(doctorIds).stream()
                        .sorted(Comparator.comparing(User::getFullName))
                        .map(LinkMapper::toDoctor)
                        .toList(),
                userRepository.findAllById(caregiverIds).stream()
                        .sorted(Comparator.comparing(User::getFullName))
                        .map(LinkMapper::toParty)
                        .toList());
    }
}
