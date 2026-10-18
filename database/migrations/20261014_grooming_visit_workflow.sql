-- Run once for manually managed MySQL databases. Normal ddl-auto=update adds these.
ALTER TABLE grooming_appointments
    ADD COLUMN preferred_finish VARCHAR(30) NULL,
    ADD COLUMN completed_tasks VARCHAR(250) NULL,
    ADD COLUMN products_used VARCHAR(500) NULL,
    ADD COLUMN next_visit_date DATE NULL;
