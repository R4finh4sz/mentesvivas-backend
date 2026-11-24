package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GeofenceRepository extends JpaRepository<Geofence, UUID> {}
