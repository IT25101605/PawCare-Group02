-- PawCare 2026-09-22: grooming booking reliability upgrade.
-- New bookings keep the full service selection on grooming_appointments itself so a
-- booking does not depend on the legacy grooming_appointment_services join table.
-- Keep complete service-name snapshots when migrating a longer legacy package.
SET SESSION group_concat_max_len=8192;

SET @has_ids = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='grooming_appointments' AND COLUMN_NAME='selected_service_ids'
);
SET @sql = IF(@has_ids=0,
    'ALTER TABLE grooming_appointments ADD COLUMN selected_service_ids VARCHAR(1000) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_names = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='grooming_appointments' AND COLUMN_NAME='selected_service_names'
);
SET @sql = IF(@has_names=0,
    'ALTER TABLE grooming_appointments ADD COLUMN selected_service_names VARCHAR(2000) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_duration = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='grooming_appointments' AND COLUMN_NAME='total_duration_minutes'
);
SET @sql = IF(@has_duration=0,
    'ALTER TABLE grooming_appointments ADD COLUMN total_duration_minutes INT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_join = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='grooming_appointment_services'
);
SET @sql = IF(@has_join>0,
'UPDATE grooming_appointments ga
 JOIN (
     SELECT gas.grooming_appointment_id AS appointment_id,
            GROUP_CONCAT(gas.grooming_service_id ORDER BY gas.grooming_service_id SEPARATOR '','') AS service_ids,
            GROUP_CONCAT(gs.name ORDER BY gas.grooming_service_id SEPARATOR '' + '') AS service_names,
            SUM(COALESCE(gs.duration_minutes,0)) AS duration_minutes
     FROM grooming_appointment_services gas
     JOIN grooming_services gs ON gs.id=gas.grooming_service_id
     GROUP BY gas.grooming_appointment_id
 ) totals ON totals.appointment_id=ga.id
 SET ga.selected_service_ids=totals.service_ids,
     ga.selected_service_names=totals.service_names,
     ga.total_duration_minutes=CASE WHEN totals.duration_minutes>0 THEN GREATEST(15,totals.duration_minutes) ELSE ga.total_duration_minutes END
 WHERE totals.service_ids IS NOT NULL AND (ga.selected_service_ids IS NULL OR TRIM(ga.selected_service_ids)='''')',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fill older single-service bookings after preserving any legacy multi-service selection.
UPDATE grooming_appointments ga
LEFT JOIN grooming_services gs ON gs.id=ga.service_id
SET ga.selected_service_ids=COALESCE(NULLIF(TRIM(ga.selected_service_ids),''),CAST(ga.service_id AS CHAR)),
    ga.selected_service_names=COALESCE(NULLIF(TRIM(ga.selected_service_names),''),gs.name),
    ga.total_duration_minutes=CASE
        WHEN ga.total_duration_minutes IS NULL OR ga.total_duration_minutes<=0
        THEN GREATEST(15,COALESCE(gs.duration_minutes,30))
        ELSE ga.total_duration_minutes END
WHERE ga.service_id IS NOT NULL;

