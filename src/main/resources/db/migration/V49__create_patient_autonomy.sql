CREATE TABLE patient_autonomy (
    patient_id             UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    mode                   VARCHAR(12) NOT NULL CHECK (mode IN ('AUTONOMOUS', 'ASSISTED')),
    restrict_care_records BOOLEAN NOT NULL,
    restrict_games        BOOLEAN NOT NULL,
    configured_by         UUID NOT NULL REFERENCES users (id),
    configured_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (mode = 'ASSISTED' OR (NOT restrict_care_records AND NOT restrict_games))
);
