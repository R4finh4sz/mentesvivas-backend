CREATE TABLE patient_profiles (
    user_id            UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    blood_type         VARCHAR(15),
    declared_disease   VARCHAR(300),
    declared_disease_validated BOOLEAN NOT NULL DEFAULT FALSE,
    allergy_status     VARCHAR(20) NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE allergies (
    id           UUID PRIMARY KEY,
    patient_id   UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category     VARCHAR(20)  NOT NULL,
    description  VARCHAR(200) NOT NULL,
    severity     VARCHAR(10)  NOT NULL,
    created_by   UUID         NOT NULL REFERENCES users (id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_allergies_patient ON allergies (patient_id);

CREATE TABLE body_measurements (
    id           UUID PRIMARY KEY,
    patient_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    weight_kg    NUMERIC(5, 1),
    height_m     NUMERIC(3, 2),
    recorded_by  UUID NOT NULL REFERENCES users (id),
    measured_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_body_measurements_patient ON body_measurements (patient_id, measured_at);

CREATE TABLE emergency_contacts (
    id             UUID PRIMARY KEY,
    patient_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name           VARCHAR(150) NOT NULL,
    relationship   VARCHAR(50)  NOT NULL,
    phone          VARCHAR(11)  NOT NULL,
    contact_order  INTEGER      NOT NULL,
    CONSTRAINT uk_emergency_contacts_order UNIQUE (patient_id, contact_order)
);

CREATE TABLE sensitive_data_changes (
    id          UUID PRIMARY KEY,
    patient_id  UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    field       VARCHAR(30)  NOT NULL,
    change_type VARCHAR(15)  NOT NULL,
    summary     VARCHAR(300) NOT NULL,
    changed_by  UUID         NOT NULL REFERENCES users (id),
    changed_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_sensitive_data_changes_patient ON sensitive_data_changes (patient_id, changed_at);
