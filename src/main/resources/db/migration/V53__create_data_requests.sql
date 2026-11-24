CREATE TABLE data_requests (
    id           UUID PRIMARY KEY,
    requested_by UUID NOT NULL REFERENCES users (id),
    request_type VARCHAR(12) NOT NULL CHECK (request_type IN ('ACCESS', 'CORRECTION', 'DELETION')),
    status       VARCHAR(12) NOT NULL CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DECLINED')),
    opened_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_data_requests_user_date ON data_requests (requested_by, opened_at, id);
CREATE INDEX idx_data_requests_status_date ON data_requests (status, opened_at, id);
