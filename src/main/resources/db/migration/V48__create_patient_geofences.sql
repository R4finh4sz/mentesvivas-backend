CREATE TABLE patient_geofences (
    patient_id    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    latitude      DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude     DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    radius_meters INT NOT NULL CHECK (radius_meters > 0),
    last_position VARCHAR(10) NOT NULL CHECK (last_position IN ('UNKNOWN', 'INSIDE', 'OUTSIDE'))
);
