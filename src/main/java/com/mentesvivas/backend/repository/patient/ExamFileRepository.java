package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.ExamFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExamFileRepository extends JpaRepository<ExamFile, UUID> {
}
