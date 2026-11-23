CREATE TABLE appointments (
    id           UUID PRIMARY KEY,
    patient_id   UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(15)   NOT NULL,
    title        VARCHAR(120)  NOT NULL,
    location     VARCHAR(200),
    notes        VARCHAR(500),
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    remind_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by   UUID          NOT NULL REFERENCES users (id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_appointments_patient ON appointments (patient_id, scheduled_at);
