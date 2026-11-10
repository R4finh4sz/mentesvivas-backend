package com.mentesvivas.backend.repository.link;

import com.mentesvivas.backend.entity.link.InvitationKind;
import com.mentesvivas.backend.entity.link.PendingInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PendingInvitationRepository extends JpaRepository<PendingInvitation, UUID> {

    boolean existsByEmailAndInviterIdAndKind(String email, UUID inviterId, InvitationKind kind);

    List<PendingInvitation> findByEmail(String email);
}
