package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.dto.link.CaregiverLinkResponse;
import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.InvitationKind;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.link.PendingInvitation;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.mapper.link.LinkMapper;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.repository.link.PendingInvitationRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.security.RoleRequirement;
import com.mentesvivas.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaregiverLinkService {

    private final CurrentUser currentUser;
    private final RoleRequirement roles;
    private final UserRepository userRepository;
    private final CaregiverPatientLinkRepository linkRepository;
    private final PendingInvitationRepository pendingRepository;
    private final NotificationService notifications;
    private final LinkEmails emails;

    @Transactional
    public void invite(String email) {
        User caregiver = roles.require(Role.CAREGIVER);
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            if (!pendingRepository.existsByEmailAndInviterIdAndKind(email, caregiver.getId(), InvitationKind.CAREGIVER_TO_PATIENT)) {
                pendingRepository.save(new PendingInvitation(email, caregiver.getId(), InvitationKind.CAREGIVER_TO_PATIENT));
            }
            emails.inviteToRegister(email);
            return;
        }
        User patient = found.get();
        if (patient.getRole() == Role.PATIENT && !patient.getId().equals(caregiver.getId())) {
            open(caregiver, patient);
        }
    }

    @Transactional
    public void openFromPendingInvitation(UUID caregiverId, User patient) {
        userRepository.findById(caregiverId)
                .filter(user -> user.getRole() == Role.CAREGIVER)
                .ifPresent(caregiver -> open(caregiver, patient));
    }

    @Transactional
    public CaregiverLinkResponse accept(UUID linkId) {
        User patient = roles.require(Role.PATIENT);
        CaregiverPatientLink link = pendingLinkOf(linkId, patient);
        link.setStatus(LinkStatus.ACTIVE);
        notifications.notify(link.getCaregiverId(), NotificationType.LINK_ACCEPTED, link.getId(),
                "%s aceitou seu convite de cuidador.".formatted(patient.getFullName()));
        return toResponse(link);
    }

    @Transactional
    public CaregiverLinkResponse decline(UUID linkId) {
        User patient = roles.require(Role.PATIENT);
        CaregiverPatientLink link = pendingLinkOf(linkId, patient);
        link.setStatus(LinkStatus.DECLINED);
        notifications.notify(link.getCaregiverId(), NotificationType.LINK_DECLINED, link.getId(),
                "%s recusou seu convite de cuidador.".formatted(patient.getFullName()));
        return toResponse(link);
    }

    @Transactional
    public CaregiverLinkResponse end(UUID linkId) {
        User caller = currentUser.get();
        CaregiverPatientLink link = find(linkId);
        boolean isCaregiver = caller.getId().equals(link.getCaregiverId());
        boolean isPatient = caller.getId().equals(link.getPatientId());
        if (!isCaregiver && !isPatient) {
            throw new AccessDeniedException("Voce nao participa deste vinculo.");
        }
        if (!link.getStatus().isOpen()) {
            throw new ConflictException("Este vinculo ja foi encerrado ou recusado.");
        }
        link.setStatus(LinkStatus.ENDED);
        UUID other = isCaregiver ? link.getPatientId() : link.getCaregiverId();
        notifications.notify(other, NotificationType.LINK_ENDED, link.getId(),
                "%s encerrou o vinculo de cuidado.".formatted(caller.getFullName()));
        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<CaregiverLinkResponse> mine() {
        User caller = currentUser.get();
        List<CaregiverPatientLink> links = switch (caller.getRole()) {
            case CAREGIVER -> linkRepository.findByCaregiverIdOrderByCreatedAtDesc(caller.getId());
            case PATIENT -> linkRepository.findByPatientIdOrderByCreatedAtDesc(caller.getId());
            default -> throw new AccessDeniedException("Acesso restrito a pacientes e cuidadores.");
        };
        return links.stream().map(this::toResponse).toList();
    }

    private void open(User caregiver, User patient) {
        CaregiverPatientLink link = linkRepository.findByCaregiverIdAndPatientId(caregiver.getId(), patient.getId())
                .orElseGet(() -> new CaregiverPatientLink(caregiver.getId(), patient.getId(), LinkStatus.PENDING));
        if (link.getId() != null && link.getStatus().isOpen()) {
            return;
        }
        link.setStatus(LinkStatus.PENDING);
        CaregiverPatientLink saved = linkRepository.save(link);
        notifications.notify(patient.getId(), NotificationType.CAREGIVER_INVITATION, saved.getId(),
                "%s convidou voce para ser seu cuidador.".formatted(caregiver.getFullName()));
        emails.inviteRegistered(patient.getEmail());
    }

    private CaregiverPatientLink pendingLinkOf(UUID linkId, User patient) {
        CaregiverPatientLink link = find(linkId);
        if (!link.getPatientId().equals(patient.getId())) {
            throw new AccessDeniedException("Este convite nao e seu.");
        }
        if (link.getStatus() != LinkStatus.PENDING) {
            throw new ConflictException("Este convite nao esta mais pendente.");
        }
        return link;
    }

    private CaregiverPatientLink find(UUID linkId) {
        return linkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Vinculo nao encontrado."));
    }

    private CaregiverLinkResponse toResponse(CaregiverPatientLink link) {
        User caregiver = userRepository.findById(link.getCaregiverId()).orElseThrow();
        User patient = userRepository.findById(link.getPatientId()).orElseThrow();
        return LinkMapper.toResponse(link, caregiver, patient);
    }
}
