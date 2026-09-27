ALTER TABLE background_check
    ADD COLUMN next_poll_at TIMESTAMP(6) NULL,
    ADD COLUMN tracking_stop_reason VARCHAR(100) NULL,
    ADD INDEX idx_background_check_next_poll (next_poll_at);
UPDATE background_check SET next_poll_at = CURRENT_TIMESTAMP(6)
WHERE status = 'PENDING' AND external_check_id IS NOT NULL;
