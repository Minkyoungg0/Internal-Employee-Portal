-- TIMESTAMP conversion must not depend on the connection's original time zone.
SET @retention_original_time_zone = @@session.time_zone;
SET time_zone = '+00:00';

UPDATE background_check
SET expires_at = DATE_SUB(
    DATE_ADD(DATE(DATE_ADD(COALESCE(completed_at, requested_at), INTERVAL 9 HOUR)), INTERVAL 90 DAY),
    INTERVAL 9 HOUR);

SET time_zone = @retention_original_time_zone;
