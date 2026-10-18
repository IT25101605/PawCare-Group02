-- PawCare 2026-09-21
-- Reviews publish immediately; old rejected reviews are preserved as hidden.
-- Safe to run once on an existing PawCare database after the earlier review migration.

UPDATE reviews
SET is_hidden = TRUE
WHERE moderation_status = 'REJECTED' AND (is_hidden = FALSE OR is_hidden IS NULL);

UPDATE reviews
SET moderation_status = 'APPROVED', is_approved = TRUE
WHERE moderation_status IS NULL OR moderation_status IN ('PENDING','REJECTED');

ALTER TABLE reviews MODIFY is_approved BOOLEAN DEFAULT TRUE;
ALTER TABLE reviews MODIFY moderation_status ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'APPROVED';
