ALTER TABLE users ADD COLUMN photo_url VARCHAR(500);
ALTER TABLE users ADD COLUMN mfa_method VARCHAR(20) NOT NULL DEFAULT 'EMAIL';
ALTER TABLE users ADD COLUMN totp_secret VARCHAR(200);

ALTER TABLE terms_versions ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'TERMS_OF_USE';
ALTER TABLE terms_versions ADD COLUMN audience VARCHAR(10) NOT NULL DEFAULT 'ALL';
ALTER TABLE terms_versions ADD COLUMN audience_roles VARCHAR(100);

CREATE TABLE doctor_patient_links (
    id          UUID PRIMARY KEY,
    doctor_id   UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    patient_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status      VARCHAR(10) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_doctor_patient_links UNIQUE (doctor_id, patient_id)
);
