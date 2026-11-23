package com.mentesvivas.backend.repository.prescription;

import com.mentesvivas.backend.entity.prescription.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {

    List<PrescriptionItem> findByPrescriptionIdOrderByPositionAsc(UUID prescriptionId);

    Optional<PrescriptionItem> findByIdAndPrescriptionId(UUID id, UUID prescriptionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PrescriptionItem i where i.prescriptionId = :prescriptionId")
    void deleteAllByPrescriptionId(UUID prescriptionId);
}
