CREATE TABLE medications (
    id              UUID PRIMARY KEY,
    patient_id      UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name            VARCHAR(150)  NOT NULL,
    strength        VARCHAR(100),
    units_per_dose  NUMERIC(6, 2) NOT NULL,
    stock_quantity  NUMERIC(8, 2) NOT NULL,
    origin          VARCHAR(15)   NOT NULL,
    active          BOOLEAN       NOT NULL,
    created_by      UUID          NOT NULL REFERENCES users (id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_medications_patient ON medications (patient_id);

CREATE TABLE medication_times (
    medication_id UUID NOT NULL REFERENCES medications (id) ON DELETE CASCADE,
    time_of_day   TIME NOT NULL,
    PRIMARY KEY (medication_id, time_of_day)
);

CREATE TABLE stock_adjustments (
    id                UUID PRIMARY KEY,
    medication_id     UUID          NOT NULL REFERENCES medications (id) ON DELETE CASCADE,
    previous_quantity NUMERIC(8, 2) NOT NULL,
    new_quantity      NUMERIC(8, 2) NOT NULL,
    justification     VARCHAR(300)  NOT NULL,
    adjusted_by       UUID          NOT NULL REFERENCES users (id),
    adjusted_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_stock_adjustments_medication ON stock_adjustments (medication_id, adjusted_at);
