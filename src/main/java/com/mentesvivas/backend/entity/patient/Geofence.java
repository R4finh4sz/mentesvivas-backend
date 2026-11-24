package com.mentesvivas.backend.entity.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "patient_geofences")
@Getter
@NoArgsConstructor
public class Geofence {

    public enum Position { UNKNOWN, INSIDE, OUTSIDE }

    @Id
    private UUID patientId;
    @Column(nullable = false)
    private double latitude;
    @Column(nullable = false)
    private double longitude;
    @Column(nullable = false)
    private int radiusMeters;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Position lastPosition;

    public Geofence(UUID patientId) {
        this.patientId = patientId;
    }

    public boolean configure(double latitude, double longitude, int radiusMeters) {
        if (this.latitude == latitude && this.longitude == longitude && this.radiusMeters == radiusMeters) {
            return false;
        }
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        lastPosition = Position.UNKNOWN;
        return true;
    }

    public boolean updatePosition(Position position) {
        if (position == Position.UNKNOWN || lastPosition == position) {
            return false;
        }
        lastPosition = position;
        return position == Position.OUTSIDE;
    }
}
