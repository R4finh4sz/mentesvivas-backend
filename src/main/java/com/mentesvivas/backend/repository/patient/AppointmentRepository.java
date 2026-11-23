package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByIdAndPatientId(UUID id, UUID patientId);

    List<Appointment> findByPatientIdAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(UUID patientId, Instant from);

    List<Appointment> findByPatientIdAndScheduledAtBetweenOrderByScheduledAtAsc(UUID patientId, Instant from,
                                                                                Instant to);
}
