CREATE TABLE cid10_catalog (
    code        VARCHAR(5) PRIMARY KEY,
    description VARCHAR(400) NOT NULL
);

CREATE TABLE diagnosis_revisions (
    id          UUID PRIMARY KEY,
    patient_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recorded_by UUID NOT NULL REFERENCES users (id),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    disease     VARCHAR(300) NOT NULL,
    primary_cid VARCHAR(5) REFERENCES cid10_catalog (code)
);

CREATE INDEX idx_diagnosis_revisions_patient_date ON diagnosis_revisions (patient_id, recorded_at, id);

CREATE TABLE diagnosis_secondary_codes (
    revision_id UUID NOT NULL REFERENCES diagnosis_revisions (id) ON DELETE CASCADE,
    position    INT NOT NULL,
    code        VARCHAR(5) NOT NULL REFERENCES cid10_catalog (code),
    PRIMARY KEY (revision_id, position),
    UNIQUE (revision_id, code)
);
