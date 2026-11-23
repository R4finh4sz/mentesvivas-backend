CREATE TABLE drug_catalog (
    id                UUID PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,
    search_key        VARCHAR(150) NOT NULL,
    active_ingredient VARCHAR(150) NOT NULL,
    ingredient_key    VARCHAR(150) NOT NULL,
    therapeutic_class VARCHAR(100),
    controlled        BOOLEAN      NOT NULL,
    active            BOOLEAN      NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_drug_catalog_search_key UNIQUE (search_key)
);

CREATE INDEX idx_drug_catalog_ingredient ON drug_catalog (ingredient_key);

CREATE TABLE prescriptions (
    id                        UUID PRIMARY KEY,
    patient_id                UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    doctor_id                 UUID         NOT NULL REFERENCES users (id),
    kind                      VARCHAR(20)  NOT NULL,
    status                    VARCHAR(10)  NOT NULL,
    orientations              VARCHAR(2000),
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    signed_at                 TIMESTAMP WITH TIME ZONE,
    valid_until               TIMESTAMP WITH TIME ZONE,
    verification_code         VARCHAR(40),
    signature_reference       VARCHAR(200),
    signature_simulated       BOOLEAN      NOT NULL,
    replaces_id               UUID REFERENCES prescriptions (id),
    replacement_justification VARCHAR(500),
    replaced_by_id            UUID REFERENCES prescriptions (id),
    last_signature_error      VARCHAR(300),
    expiry_notice_sent_at     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_prescriptions_verification_code UNIQUE (verification_code)
);

CREATE INDEX idx_prescriptions_patient ON prescriptions (patient_id, created_at);
CREATE INDEX idx_prescriptions_doctor ON prescriptions (doctor_id, patient_id);

CREATE TABLE prescription_items (
    id                             UUID PRIMARY KEY,
    prescription_id                UUID         NOT NULL REFERENCES prescriptions (id) ON DELETE CASCADE,
    position                       INT          NOT NULL,
    medication_name                VARCHAR(150),
    presentation                   VARCHAR(100),
    dose                           VARCHAR(100),
    route                          VARCHAR(60),
    frequency                      VARCHAR(100),
    duration                       VARCHAR(100),
    quantity                       VARCHAR(60),
    instructions                   VARCHAR(500),
    continuous_use                 BOOLEAN      NOT NULL,
    allergy_override_justification VARCHAR(300)
);

CREATE INDEX idx_prescription_items_prescription ON prescription_items (prescription_id, position);

CREATE TABLE prescription_documents (
    prescription_id UUID PRIMARY KEY REFERENCES prescriptions (id) ON DELETE CASCADE,
    content         BYTEA NOT NULL
);

ALTER TABLE medications ADD COLUMN prescription_id UUID REFERENCES prescriptions (id);
