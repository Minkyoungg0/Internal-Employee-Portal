ALTER TABLE background_check ADD COLUMN expires_at TIMESTAMP(6) NULL;

UPDATE background_check
SET expires_at = DATE_ADD(COALESCE(completed_at, requested_at), INTERVAL 90 DAY);

ALTER TABLE background_check
    MODIFY COLUMN expires_at TIMESTAMP(6) NOT NULL,
    ADD INDEX idx_background_check_expires_at (expires_at);

-- Apply the same policy to data created before retention was introduced.
DELETE c FROM background_check c JOIN employee e ON e.id = c.employee_id
WHERE e.employment_status = 'TERMINATED';
DELETE FROM background_check WHERE expires_at <= CURRENT_TIMESTAMP(6);
