CREATE TABLE occurrences (
    id             UUID PRIMARY KEY,
    patient_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type           VARCHAR(15)   NOT NULL,
    severity       VARCHAR(10)   NOT NULL,
    description    VARCHAR(1000) NOT NULL,
    needs_hospital BOOLEAN       NOT NULL,
    source         VARCHAR(10)   NOT NULL,
    recorded_by    UUID          NOT NULL REFERENCES users (id),
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_occurrences_patient ON occurrences (patient_id, occurred_at);
