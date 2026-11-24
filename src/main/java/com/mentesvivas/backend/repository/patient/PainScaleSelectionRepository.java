package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.PainScaleSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PainScaleSelectionRepository extends JpaRepository<PainScaleSelection, UUID> {}
