CREATE TABLE routine_records (
    id                UUID PRIMARY KEY,
    patient_id        UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type              VARCHAR(15)  NOT NULL,
    mood              VARCHAR(15),
    sleep_quality     VARCHAR(10),
    sleep_hours       NUMERIC(3, 1),
    meal_period       VARCHAR(15),
    meal_accepted     BOOLEAN,
    amount_ml         INTEGER,
    bath_cooperation  VARCHAR(15),
    urine_frequency   INTEGER,
    urine_aspect      VARCHAR(10),
    stool_frequency   INTEGER,
    bristol_scale     INTEGER,
    notes             VARCHAR(500),
    source            VARCHAR(10)  NOT NULL,
    recorded_by       UUID         NOT NULL REFERENCES users (id),
    occurred_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_routine_records_patient ON routine_records (patient_id, occurred_at);
