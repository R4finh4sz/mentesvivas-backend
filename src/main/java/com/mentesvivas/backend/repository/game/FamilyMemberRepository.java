package com.mentesvivas.backend.repository.game;

import com.mentesvivas.backend.entity.game.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {

    List<FamilyMember> findByPatientIdOrderByNameAsc(UUID patientId);

    Optional<FamilyMember> findByIdAndPatientId(UUID id, UUID patientId);

    long countByPatientId(UUID patientId);
}
