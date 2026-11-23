package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.Exam;
import com.mentesvivas.backend.entity.patient.ExamCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    Optional<Exam> findByIdAndPatientId(UUID id, UUID patientId);

    List<Exam> findByPatientIdOrderByExamDateDescCreatedAtDesc(UUID patientId);

    List<Exam> findByPatientIdAndCategoryOrderByExamDateDescCreatedAtDesc(UUID patientId, ExamCategory category);
}
