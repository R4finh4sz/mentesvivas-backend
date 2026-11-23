CREATE TABLE dose_records (
    id               UUID PRIMARY KEY,
    medication_id    UUID          NOT NULL REFERENCES medications (id) ON DELETE CASCADE,
    patient_id       UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    scheduled_for    TIMESTAMP WITH TIME ZONE NOT NULL,
    status           VARCHAR(15)   NOT NULL,
    stock_discounted NUMERIC(6, 2) NOT NULL,
    duplicate        BOOLEAN       NOT NULL,
    corrected        BOOLEAN       NOT NULL,
    recorded_by      UUID          NOT NULL REFERENCES users (id),
    recorded_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_dose_records_medication_slot ON dose_records (medication_id, scheduled_for);
CREATE INDEX idx_dose_records_patient_slot ON dose_records (patient_id, scheduled_for);

CREATE TABLE dose_corrections (
    id              UUID PRIMARY KEY,
    dose_id         UUID         NOT NULL REFERENCES dose_records (id) ON DELETE CASCADE,
    previous_status VARCHAR(15)  NOT NULL,
    new_status      VARCHAR(15)  NOT NULL,
    justification   VARCHAR(300) NOT NULL,
    corrected_by    UUID         NOT NULL REFERENCES users (id),
    corrected_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_dose_corrections_dose ON dose_corrections (dose_id, corrected_at);
