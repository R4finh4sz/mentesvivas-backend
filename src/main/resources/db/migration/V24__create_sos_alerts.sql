CREATE TABLE sos_alerts (
    id              UUID PRIMARY KEY,
    patient_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status          VARCHAR(15)  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    seen_at         TIMESTAMP WITH TIME ZONE,
    seen_by         UUID REFERENCES users (id),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by UUID REFERENCES users (id),
    resolved_at     TIMESTAMP WITH TIME ZONE,
    resolved_by     UUID REFERENCES users (id)
);

CREATE INDEX idx_sos_alerts_patient ON sos_alerts (patient_id, created_at);
