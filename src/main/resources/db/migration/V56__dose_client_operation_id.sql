ALTER TABLE dose_records ADD COLUMN client_operation_id UUID;

CREATE UNIQUE INDEX idx_dose_records_client_operation
    ON dose_records (medication_id, recorded_by, client_operation_id);
