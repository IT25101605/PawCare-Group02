-- Normal startup with ddl-auto=update creates this nullable column automatically.
-- For manually managed databases, run once before deploying the grooming planner.
ALTER TABLE grooming_appointments ADD COLUMN comfort_profile VARCHAR(20) NULL;
