ALTER TABLE nomis_sync_payloads
    ADD COLUMN offender_id BIGINT;

CREATE INDEX idx_nomis_sync_payloads_offender_id ON nomis_sync_payloads (offender_id);