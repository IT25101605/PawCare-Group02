USE pawcare_db;

-- PawCare 2026-09-21: multiple grooming services per appointment + Sri Lankan grooming catalogue prices.
-- The existing grooming_appointments.service_id remains untouched as the legacy/primary service,
-- so old code, IDs, reviews and existing bookings continue to work.
CREATE TABLE IF NOT EXISTS grooming_appointment_services (
    grooming_appointment_id BIGINT NOT NULL,
    grooming_service_id BIGINT NOT NULL,
    PRIMARY KEY (grooming_appointment_id, grooming_service_id),
    CONSTRAINT fk_groom_appt_services_appt FOREIGN KEY (grooming_appointment_id) REFERENCES grooming_appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_groom_appt_services_service FOREIGN KEY (grooming_service_id) REFERENCES grooming_services(id),
    INDEX idx_groom_appt_services_service (grooming_service_id)
) ENGINE=InnoDB;

-- Persist the complete appointment length. Hibernate ddl-auto=update also creates this
-- column automatically; this migration keeps manual database upgrades explicit.
SET @duration_col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='grooming_appointments'
      AND COLUMN_NAME='total_duration_minutes'
);
SET @duration_sql := IF(@duration_col_exists=0,
    'ALTER TABLE grooming_appointments ADD COLUMN total_duration_minutes INT NULL',
    'SELECT 1');
PREPARE duration_stmt FROM @duration_sql;
EXECUTE duration_stmt;
DEALLOCATE PREPARE duration_stmt;

-- Backfill every existing grooming booking with its original service.
INSERT IGNORE INTO grooming_appointment_services (grooming_appointment_id, grooming_service_id)
SELECT id, service_id FROM grooming_appointments WHERE service_id IS NOT NULL;

UPDATE grooming_appointments ga
LEFT JOIN grooming_services gs ON gs.id=ga.service_id
SET ga.total_duration_minutes=GREATEST(15,COALESCE(gs.duration_minutes,30))
WHERE ga.total_duration_minutes IS NULL OR ga.total_duration_minutes<=0;

UPDATE grooming_appointments ga
JOIN (
    SELECT gas.grooming_appointment_id AS appointment_id,
           SUM(COALESCE(gs.duration_minutes,0)) AS duration_minutes
    FROM grooming_appointment_services gas
    JOIN grooming_services gs ON gs.id=gas.grooming_service_id
    GROUP BY gas.grooming_appointment_id
) totals ON totals.appointment_id=ga.id
SET ga.total_duration_minutes=GREATEST(15,totals.duration_minutes)
WHERE totals.duration_minutes>0;

-- Update the existing catalogue to practical Sri Lankan Rupee values used by the application.
UPDATE grooming_services SET price=2800.00, duration_minutes=45,
    description='Shampoo bath, blow dry and brushing' WHERE LOWER(name)=LOWER('Basic Bath');
UPDATE grooming_services SET price=6000.00, duration_minutes=90,
    description='Full-body trim with bath, brushing and finishing care' WHERE LOWER(name)=LOWER('Full Grooming');
UPDATE grooming_services SET price=500.00, duration_minutes=15,
    description='Careful nail clipping and paw check' WHERE LOWER(name)=LOWER('Nail Trimming');

INSERT INTO grooming_services (name, description, duration_minutes, price, is_active)
SELECT 'Ear Cleaning','Gentle external ear cleaning',15,500.00,TRUE
WHERE NOT EXISTS (SELECT 1 FROM grooming_services WHERE LOWER(name)=LOWER('Ear Cleaning'));
INSERT INTO grooming_services (name, description, duration_minutes, price, is_active)
SELECT 'Teeth Cleaning','Basic teeth cleaning and mouth freshening',15,450.00,TRUE
WHERE NOT EXISTS (SELECT 1 FROM grooming_services WHERE LOWER(name)=LOWER('Teeth Cleaning'));
INSERT INTO grooming_services (name, description, duration_minutes, price, is_active)
SELECT 'Hygiene Trim','Sanitary, paw-pad and eye-area hygiene trim',30,1500.00,TRUE
WHERE NOT EXISTS (SELECT 1 FROM grooming_services WHERE LOWER(name)=LOWER('Hygiene Trim'));

UPDATE grooming_services SET price=1800.00, duration_minutes=45,
    description='Specialized de-shedding shampoo and brush-out' WHERE LOWER(name)=LOWER('De-shedding Treatment');
UPDATE grooming_services SET price=2200.00, duration_minutes=45,
    description='Medicated bath and coat care for flea/tick treatment' WHERE LOWER(name)=LOWER('Flea & Tick Treatment');


INSERT INTO grooming_services (name, description, duration_minutes, price, is_active)
SELECT 'De-shedding Treatment','Specialized de-shedding shampoo and brush-out',45,1800.00,TRUE
WHERE NOT EXISTS (SELECT 1 FROM grooming_services WHERE LOWER(name)=LOWER('De-shedding Treatment'));
INSERT INTO grooming_services (name, description, duration_minutes, price, is_active)
SELECT 'Flea & Tick Treatment','Medicated bath and coat care for flea/tick treatment',45,2200.00,TRUE
WHERE NOT EXISTS (SELECT 1 FROM grooming_services WHERE LOWER(name)=LOWER('Flea & Tick Treatment'));
