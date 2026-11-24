ALTER TABLE sos_alerts ADD COLUMN type VARCHAR(32) NOT NULL DEFAULT 'SOS_ALERT';
ALTER TABLE sos_alerts ADD COLUMN source_id UUID;
ALTER TABLE sos_alerts ADD COLUMN created_by UUID REFERENCES users (id);

UPDATE sos_alerts SET created_by = patient_id;
ALTER TABLE sos_alerts ALTER COLUMN created_by SET NOT NULL;
