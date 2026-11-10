package com.mentesvivas.backend.repository.link;

import com.mentesvivas.backend.entity.link.DoctorPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorPatientLinkRepository extends JpaRepository<DoctorPatientLink, UUID> {

    boolean existsByDoctorIdAndPatientIdAndStatus(UUID doctorId, UUID patientId, LinkStatus status);

    Optional<DoctorPatientLink> findByDoctorIdAndPatientId(UUID doctorId, UUID patientId);

    List<DoctorPatientLink> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId);

    List<DoctorPatientLink> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<DoctorPatientLink> findByPatientIdInOrderByCreatedAtDesc(Collection<UUID> patientIds);
}
