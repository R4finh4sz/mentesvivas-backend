package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.dto.link.DoctorLinkResponse;
import com.mentesvivas.backend.dto.link.RequestDoctorLinkRequest;
import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.DoctorPatientLink;
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
import com.mentesvivas.backend.repository.link.DoctorPatientLinkRepository;
import com.mentesvivas.backend.repository.link.PendingInvitationRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.security.RoleRequirement;
import com.mentesvivas.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorLinkService {

    private final CurrentUser currentUser;
    private final RoleRequirement roles;
    private final UserRepository userRepository;
    private final DoctorPatientLinkRepository linkRepository;
    private final CaregiverPatientLinkRepository caregiverLinkRepository;
    private final PendingInvitationRepository pendingRepository;
    private final NotificationService notifications;
    private final LinkEmails emails;

    @Transactional
    public DoctorLinkResponse request(RequestDoctorLinkRequest request) {
        User caller = currentUser.get();
        User patient = patientFor(caller, request.patientId());
        User doctor = userRepository.findById(request.doctorId())
                .filter(user -> user.getRole() == Role.DOCTOR && user.isCrmVerified())
                .orElseThrow(() -> new ResourceNotFoundException("Medico nao encontrado."));

        DoctorPatientLink link = startLink(doctor, patient, caller);
        link.setPatientAuthorized(caller.getRole() == Role.PATIENT);
        DoctorPatientLink saved = linkRepository.save(link);

        notifications.notify(doctor.getId(), NotificationType.DOCTOR_LINK_REQUEST, saved.getId(),
                "%s pediu vinculo com voce.".formatted(patient.getFullName()));
        if (caller.getRole() == Role.CAREGIVER) {
            notifications.notify(patient.getId(), NotificationType.DOCTOR_LINK_REQUEST, saved.getId(),
                    "%s pediu vinculo com o medico %s em seu nome. Autorize ou recuse.".formatted(
                            caller.getFullName(), doctor.getFullName()));
        }
        return toResponse(saved);
    }

    @Transactional
    public void invitePatient(String email) {
        User doctor = roles.require(Role.DOCTOR);
        if (!doctor.isCrmVerified()) {
            throw new AccessDeniedException("O registro profissional ainda nao foi conferido.");
        }
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            if (!pendingRepository.existsByEmailAndInviterIdAndKind(email, doctor.getId(), InvitationKind.DOCTOR_TO_PATIENT)) {
                pendingRepository.save(new PendingInvitation(email, doctor.getId(), InvitationKind.DOCTOR_TO_PATIENT));
            }
            emails.inviteToRegister(email);
            return;
        }
        if (found.get().getRole() == Role.PATIENT) {
            invite(doctor, found.get());
        }
    }

    @Transactional
    public void openFromPendingInvitation(UUID doctorId, User patient) {
        userRepository.findById(doctorId)
                .filter(user -> user.getRole() == Role.DOCTOR && user.isCrmVerified())
                .ifPresent(doctor -> invite(doctor, patient));
    }

    @Transactional
    public DoctorLinkResponse accept(UUID linkId) {
        User caller = currentUser.get();
        DoctorPatientLink link = pendingLinkOf(linkId, caller);
        if (caller.getId().equals(link.getDoctorId())) {
            link.setDoctorConfirmed(true);
        } else {
            link.setPatientAuthorized(true);
        }
        if (link.isDoctorConfirmed() && link.isPatientAuthorized()) {
            link.setStatus(LinkStatus.ACTIVE);
            notifyOthers(link, caller, NotificationType.LINK_ACCEPTED,
                    "%s confirmou o vinculo entre medico e paciente.".formatted(caller.getFullName()));
        }
        return toResponse(link);
    }

    @Transactional
    public DoctorLinkResponse decline(UUID linkId) {
        User caller = currentUser.get();
        DoctorPatientLink link = pendingLinkOf(linkId, caller);
        link.setStatus(LinkStatus.DECLINED);
        notifyOthers(link, caller, NotificationType.LINK_DECLINED,
                "%s recusou o vinculo entre medico e paciente.".formatted(caller.getFullName()));
        return toResponse(link);
    }

    @Transactional
    public DoctorLinkResponse end(UUID linkId) {
        User caller = currentUser.get();
        DoctorPatientLink link = find(linkId);
        requireParty(link, caller);
        if (!link.getStatus().isOpen()) {
            throw new ConflictException("Este vinculo ja foi encerrado ou recusado.");
        }
        link.setStatus(LinkStatus.ENDED);
        notifyOthers(link, caller, NotificationType.LINK_ENDED,
                "%s encerrou o vinculo entre medico e paciente.".formatted(caller.getFullName()));
        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<DoctorLinkResponse> mine() {
        User caller = currentUser.get();
        List<DoctorPatientLink> links = switch (caller.getRole()) {
            case DOCTOR -> linkRepository.findByDoctorIdOrderByCreatedAtDesc(caller.getId());
            case PATIENT -> linkRepository.findByPatientIdOrderByCreatedAtDesc(caller.getId());
            case CAREGIVER -> linkRepository.findByPatientIdInOrderByCreatedAtDesc(activePatientsOf(caller));
            default -> throw new AccessDeniedException("Acesso restrito a medicos, pacientes e cuidadores.");
        };
        return links.stream().map(this::toResponse).toList();
    }

    private void invite(User doctor, User patient) {
        DoctorPatientLink link = startLinkOrNull(doctor, patient, doctor);
        if (link == null) {
            return;
        }
        link.setDoctorConfirmed(true);
        link.setPatientAuthorized(false);
        DoctorPatientLink saved = linkRepository.save(link);
        notifications.notify(patient.getId(), NotificationType.DOCTOR_INVITATION, saved.getId(),
                "%s convidou voce para se vincular como seu medico.".formatted(doctor.getFullName()));
        emails.inviteRegistered(patient.getEmail());
    }

    private DoctorPatientLink startLink(User doctor, User patient, User initiator) {
        DoctorPatientLink link = startLinkOrNull(doctor, patient, initiator);
        if (link == null) {
            throw new ConflictException("Ja existe um vinculo ou pedido em andamento com este medico.");
        }
        link.setDoctorConfirmed(false);
        return link;
    }

    private DoctorPatientLink startLinkOrNull(User doctor, User patient, User initiator) {
        DoctorPatientLink link = linkRepository.findByDoctorIdAndPatientId(doctor.getId(), patient.getId())
                .orElseGet(() -> new DoctorPatientLink(doctor.getId(), patient.getId(), LinkStatus.PENDING, java.time.Instant.now()));
        if (link.getId() != null && link.getStatus().isOpen()) {
            return null;
        }
        link.setStatus(LinkStatus.PENDING);
        link.setInitiatedBy(initiator.getId());
        return link;
    }

    private User patientFor(User caller, UUID patientId) {
        return switch (caller.getRole()) {
            case PATIENT -> {
                if (patientId != null && !patientId.equals(caller.getId())) {
                    throw new AccessDeniedException("Voce so pode pedir vinculo para a sua conta.");
                }
                yield caller;
            }
            case CAREGIVER -> {
                if (patientId == null || !caregiverLinkRepository.existsByCaregiverIdAndPatientIdAndStatus(
                        caller.getId(), patientId, LinkStatus.ACTIVE)) {
                    throw new AccessDeniedException("Voce nao e cuidador ativo deste paciente.");
                }
                yield userRepository.findById(patientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Paciente nao encontrado."));
            }
            default -> throw new AccessDeniedException("Acesso restrito a pacientes e cuidadores.");
        };
    }

    private Set<UUID> activePatientsOf(User caregiver) {
        Set<UUID> ids = new HashSet<>();
        caregiverLinkRepository.findByCaregiverIdAndStatus(caregiver.getId(), LinkStatus.ACTIVE)
                .stream().map(CaregiverPatientLink::getPatientId).forEach(ids::add);
        return ids;
    }

    private DoctorPatientLink pendingLinkOf(UUID linkId, User caller) {
        DoctorPatientLink link = find(linkId);
        requireParty(link, caller);
        if (link.getStatus() != LinkStatus.PENDING) {
            throw new ConflictException("Este pedido nao esta mais pendente.");
        }
        return link;
    }

    private void requireParty(DoctorPatientLink link, User caller) {
        if (!caller.getId().equals(link.getDoctorId()) && !caller.getId().equals(link.getPatientId())) {
            throw new AccessDeniedException("Voce nao participa deste vinculo.");
        }
    }

    private void notifyOthers(DoctorPatientLink link, User caller, NotificationType type, String message) {
        Set<UUID> recipients = new HashSet<>(Set.of(link.getDoctorId(), link.getPatientId()));
        if (link.getInitiatedBy() != null) {
            recipients.add(link.getInitiatedBy());
        }
        recipients.remove(caller.getId());
        recipients.forEach(recipient -> notifications.notify(recipient, type, link.getId(), message));
    }

    private DoctorPatientLink find(UUID linkId) {
        return linkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Vinculo nao encontrado."));
    }

    private DoctorLinkResponse toResponse(DoctorPatientLink link) {
        User doctor = userRepository.findById(link.getDoctorId()).orElseThrow();
        User patient = userRepository.findById(link.getPatientId()).orElseThrow();
        return LinkMapper.toResponse(link, doctor, patient);
    }
}
