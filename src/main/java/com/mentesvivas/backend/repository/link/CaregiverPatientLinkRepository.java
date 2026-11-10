package com.mentesvivas.backend.repository.link;

import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaregiverPatientLinkRepository extends JpaRepository<CaregiverPatientLink, UUID> {

    boolean existsByCaregiverIdAndPatientIdAndStatus(UUID caregiverId, UUID patientId, LinkStatus status);

    Optional<CaregiverPatientLink> findByCaregiverIdAndPatientId(UUID caregiverId, UUID patientId);

    List<CaregiverPatientLink> findByCaregiverIdOrderByCreatedAtDesc(UUID caregiverId);

    List<CaregiverPatientLink> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<CaregiverPatientLink> findByCaregiverIdAndStatus(UUID caregiverId, LinkStatus status);
}
