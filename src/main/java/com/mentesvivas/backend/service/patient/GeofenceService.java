package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.GeofenceReading;
import com.mentesvivas.backend.dto.patient.GeofenceRequest;
import com.mentesvivas.backend.dto.patient.GeofenceResponse;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.patient.ConsentScope;
import com.mentesvivas.backend.entity.patient.Geofence;
import com.mentesvivas.backend.entity.patient.Geofence.Position;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.patient.GeofenceRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import com.mentesvivas.backend.service.alert.CriticalAlertService;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import com.mentesvivas.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GeofenceService {

    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    private final CurrentPatient currentPatient;
    private final CaregiverPatientAccess caregivers;
    private final CriticalAlertService alerts;
    private final PatientConsentService consent;
    private final GeofenceRepository fences;
    private final AuditTrail audit;
    private final NotificationService notifications;

    @Value("${app.geofence.interval-seconds:60}")
    private int readingIntervalSeconds;

    @Transactional
    public GeofenceResponse configure(UUID patientId, GeofenceRequest request) {
        UUID actor = caregivers.requireRepresentativeOf(patientId).getId();
        consent.lockForCollection(patientId);
        consent.require(patientId, ConsentScope.GEOLOCATION);
        Geofence zone = fences.findById(patientId).orElseGet(() -> new Geofence(patientId));
        boolean configured = zone.getLastPosition() != null;
        if (!zone.configure(request.latitude().doubleValue(), request.longitude().doubleValue(), request.radiusMeters())) {
            return view(zone);
        }
        Geofence saved = fences.save(zone);
        audit.record(AuditRecord.of(AuditEventType.GEOFENCE_CHANGED, actor).forPatient(patientId)
                .onBehalfOf(patientId).withValues(configured ? "ACTIVE" : null, "ACTIVE"));
        notifications.notify(patientId, NotificationType.GEOFENCE_NOTICE, patientId, "A cerca virtual está ativa.");
        return view(saved);
    }

    @Transactional
    public void disable(UUID patientId) {
        UUID actor = caregivers.requireRepresentativeOf(patientId).getId();
        consent.lockForCollection(patientId);
        fences.findById(patientId).ifPresent(zone -> {
            fences.delete(zone);
            audit.record(AuditRecord.of(AuditEventType.GEOFENCE_CHANGED, actor).forPatient(patientId)
                    .onBehalfOf(patientId).withValues("ACTIVE", "DISABLED"));
        });
    }

    @Transactional(readOnly = true)
    public GeofenceResponse current() {
        return view(load(currentPatient.get().getId()));
    }

    @Transactional
    public Position evaluate(GeofenceReading reading) {
        UUID patientId = currentPatient.get().getId();
        consent.lockForCollection(patientId);
        Geofence zone = load(patientId);
        double meters = distance(zone, reading.latitude().doubleValue(), reading.longitude().doubleValue());
        int accuracy = reading.accuracyMeters();
        Position position = Position.UNKNOWN;
        if (meters - accuracy > zone.getRadiusMeters()) {
            position = Position.OUTSIDE;
        } else if (meters + accuracy <= zone.getRadiusMeters()) {
            position = Position.INSIDE;
        }
        if (zone.updatePosition(position)) {
            alerts.create(patientId, patientId, NotificationType.GEOFENCE_EXIT, null,
                    "O paciente foi localizado fora da cerca virtual.");
        }
        return position;
    }

    private Geofence load(UUID patientId) {
        consent.require(patientId, ConsentScope.GEOLOCATION);
        return fences.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cerca virtual inativa."));
    }

    private GeofenceResponse view(Geofence zone) {
        return new GeofenceResponse(zone.getLatitude(), zone.getLongitude(), zone.getRadiusMeters(),
                Math.max(1, readingIntervalSeconds), "A cerca virtual está ativa.");
    }

    private double distance(Geofence zone, double latitude, double longitude) {
        double lat = Math.toRadians(zone.getLatitude());
        double otherLat = Math.toRadians(latitude);
        double halfLat = Math.sin((otherLat - lat) / 2);
        double halfLon = Math.sin(Math.toRadians(longitude - zone.getLongitude()) / 2);
        double haversine = halfLat * halfLat + Math.cos(lat) * Math.cos(otherLat) * halfLon * halfLon;
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(Math.min(1, haversine)));
    }
}
