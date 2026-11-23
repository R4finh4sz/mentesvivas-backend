package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.Occurrence;
import com.mentesvivas.backend.entity.patient.OccurrenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OccurrenceRepository extends JpaRepository<Occurrence, UUID> {

    List<Occurrence> findByPatientIdOrderByOccurredAtDesc(UUID patientId);

    List<Occurrence> findByPatientIdAndTypeOrderByOccurredAtDesc(UUID patientId, OccurrenceType type);

    List<Occurrence> findByPatientIdAndDescriptionContainingIgnoreCaseOrderByOccurredAtDesc(UUID patientId,
                                                                                            String keyword);

    List<Occurrence> findByPatientIdAndTypeAndDescriptionContainingIgnoreCaseOrderByOccurredAtDesc(
            UUID patientId, OccurrenceType type, String keyword);
}
