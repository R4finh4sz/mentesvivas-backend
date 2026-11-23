CREATE TABLE vital_signs (
    id                 UUID PRIMARY KEY,
    patient_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    systolic           INTEGER,
    diastolic          INTEGER,
    heart_rate         INTEGER,
    glucose            INTEGER,
    glucose_type       VARCHAR(15),
    temperature        NUMERIC(3, 1),
    oxygen_saturation  INTEGER,
    notes              VARCHAR(500),
    source             VARCHAR(10)  NOT NULL,
    recorded_by        UUID         NOT NULL REFERENCES users (id),
    measured_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_vital_signs_patient ON vital_signs (patient_id, measured_at);
