CREATE TABLE pain_scale_selections (
    patient_id  UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    scale       VARCHAR(20) NOT NULL CHECK (scale IN ('NUMERIC', 'WONG_BAKER')),
    selected_by UUID NOT NULL REFERENCES users (id),
    selected_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE pain_records (
    id          UUID PRIMARY KEY,
    patient_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    scale       VARCHAR(20) NOT NULL CHECK (scale IN ('NUMERIC', 'WONG_BAKER')),
    score       INT NOT NULL CHECK (score BETWEEN 0 AND 10 AND (scale <> 'WONG_BAKER' OR MOD(score, 2) = 0)),
    recorded_by UUID NOT NULL REFERENCES users (id),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_pain_records_patient_date ON pain_records (patient_id, recorded_at, id);
