USE pawcare_db;

-- Secure veterinarian <-> pet owner conversations.
CREATE TABLE IF NOT EXISTS message_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    veterinarian_id BIGINT NOT NULL,
    latest_appointment_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    latest_message_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_message_conversation_pet FOREIGN KEY (pet_id) REFERENCES pets(id),
    CONSTRAINT fk_message_conversation_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_message_conversation_vet FOREIGN KEY (veterinarian_id) REFERENCES users(id),
    CONSTRAINT fk_message_conversation_appt FOREIGN KEY (latest_appointment_id) REFERENCES appointments(id),
    UNIQUE KEY uk_message_conversation_pair (pet_id, owner_id, veterinarian_id),
    INDEX idx_message_conversation_latest (latest_message_at)
) ENGINE=InnoDB;

SET @has_conversation_id = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='conversation_messages' AND column_name='conversation_id');
SET @sql = IF(@has_conversation_id=0,'ALTER TABLE conversation_messages ADD COLUMN conversation_id BIGINT NULL AFTER id','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_msg_conversation_fk = (SELECT COUNT(*) FROM information_schema.referential_constraints WHERE constraint_schema=DATABASE() AND table_name='conversation_messages' AND constraint_name='fk_msg_conversation');
SET @sql = IF(@has_msg_conversation_fk=0,'ALTER TABLE conversation_messages ADD CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES message_conversations(id)','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_msg_conversation_idx = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='conversation_messages' AND index_name='idx_msg_conversation_created');
SET @sql = IF(@has_msg_conversation_idx=0,'CREATE INDEX idx_msg_conversation_created ON conversation_messages(conversation_id, created_at)','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Review ownership references, moderation, and reporting.
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='order_id');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN order_id BIGINT NULL AFTER product_id','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='grooming_appointment_id');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN grooming_appointment_id BIGINT NULL AFTER order_id','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='grooming_service_id');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN grooming_service_id BIGINT NULL AFTER grooming_appointment_id','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='groomer_id');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN groomer_id BIGINT NULL AFTER grooming_service_id','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='pet_id');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN pet_id BIGINT NULL AFTER groomer_id','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='moderation_status');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN moderation_status ENUM(''PENDING'',''APPROVED'',''REJECTED'') NULL DEFAULT ''PENDING'' AFTER is_approved','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='is_hidden');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT FALSE AFTER moderation_status','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='is_reported');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN is_reported BOOLEAN NOT NULL DEFAULT FALSE AFTER is_hidden','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='report_reason');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN report_reason VARCHAR(500) NULL AFTER is_reported','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='reported_at');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN reported_at DATETIME(6) NULL AFTER report_reason','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='updated_at');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN updated_at DATETIME(6) NULL AFTER created_at','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='moderated_at');
SET @sql = IF(@has_col=0,'ALTER TABLE reviews ADD COLUMN moderated_at DATETIME(6) NULL AFTER updated_at','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Treat legacy approved reviews as approved in the new moderation workflow.
UPDATE reviews SET moderation_status='APPROVED' WHERE is_approved=TRUE AND (moderation_status IS NULL OR moderation_status='PENDING');
UPDATE reviews SET moderation_status='PENDING' WHERE moderation_status IS NULL;

-- Add foreign keys only when they do not already exist for each review column.
SET @has_fk = (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='order_id' AND referenced_table_name IS NOT NULL);
SET @sql = IF(@has_fk=0,'ALTER TABLE reviews ADD CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(id)','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_fk = (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='grooming_appointment_id' AND referenced_table_name IS NOT NULL);
SET @sql = IF(@has_fk=0,'ALTER TABLE reviews ADD CONSTRAINT fk_review_grooming_appt FOREIGN KEY (grooming_appointment_id) REFERENCES grooming_appointments(id)','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_fk = (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='grooming_service_id' AND referenced_table_name IS NOT NULL);
SET @sql = IF(@has_fk=0,'ALTER TABLE reviews ADD CONSTRAINT fk_review_grooming_service FOREIGN KEY (grooming_service_id) REFERENCES grooming_services(id)','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_fk = (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='groomer_id' AND referenced_table_name IS NOT NULL);
SET @sql = IF(@has_fk=0,'ALTER TABLE reviews ADD CONSTRAINT fk_review_groomer FOREIGN KEY (groomer_id) REFERENCES users(id)','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_fk = (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='pet_id' AND referenced_table_name IS NOT NULL);
SET @sql = IF(@has_fk=0,'ALTER TABLE reviews ADD CONSTRAINT fk_review_pet FOREIGN KEY (pet_id) REFERENCES pets(id)','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='reviews' AND index_name='uk_review_service_once');
SET @sql = IF(@has_idx=0,'CREATE UNIQUE INDEX uk_review_service_once ON reviews(user_id, grooming_appointment_id, review_type)','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='reviews' AND index_name='uk_review_product_order_once');
SET @sql = IF(@has_idx=0,'CREATE UNIQUE INDEX uk_review_product_order_once ON reviews(user_id, product_id, order_id, review_type)','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='reviews' AND index_name='idx_review_product_public');
SET @sql = IF(@has_idx=0,'CREATE INDEX idx_review_product_public ON reviews(product_id, moderation_status, is_hidden, created_at)','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_idx = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='reviews' AND index_name='idx_review_moderation');
SET @sql = IF(@has_idx=0,'CREATE INDEX idx_review_moderation ON reviews(moderation_status, is_reported, created_at)','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
