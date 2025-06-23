-- src/main/resources/db/migration/V1__create_nomis_sync_payloads_table.sql

CREATE TABLE nomis_sync_payloads (
                                     id BIGSERIAL PRIMARY KEY,
                                     timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL, -- When the payload was captured

    -- Extracted fields from incoming request models (nullable as not all requests will have them)
                                     transaction_id INTEGER,
                                     request_id UUID, -- Using PostgreSQL's native UUID type
                                     caseload_id VARCHAR(255),

    -- General request type identifier (e.g., "SyncOffenderTransaction")
                                     request_type_identifier VARCHAR(255),

    -- The full original request body, stored as PostgreSQL's JSONB type
                                     body JSONB NOT NULL
);

-- Add indexes for frequently queried columns to improve performance
CREATE INDEX idx_nomis_sync_payloads_timestamp ON nomis_sync_payloads (timestamp);
CREATE INDEX idx_nomis_sync_payloads_request_type ON nomis_sync_payloads (request_type_identifier);
CREATE INDEX idx_nomis_sync_payloads_transaction_id ON nomis_sync_payloads (transaction_id);
    CREATE INDEX idx_nomis_sync_payloads_request_id ON nomis_sync_payloads (request_id);
CREATE INDEX idx_nomis_sync_payloads_caseload_id ON nomis_sync_payloads (caseload_id);
