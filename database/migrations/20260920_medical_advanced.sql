USE pawcare_db;

SET @has_reorder_level = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='products' AND column_name='reorder_level');
SET @sql = IF(@has_reorder_level=0,'ALTER TABLE products ADD COLUMN reorder_level INT NOT NULL DEFAULT 0 AFTER stock_quantity','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_record_type = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='medical_records' AND column_name='record_type');
SET @sql = IF(@has_record_type=0,'ALTER TABLE medical_records ADD COLUMN record_type VARCHAR(30) NULL AFTER record_date','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS medical_attachments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 medical_record_id BIGINT NOT NULL,
 uploaded_by BIGINT NOT NULL,
 file_name VARCHAR(255) NOT NULL,
 file_url VARCHAR(500) NOT NULL,
 content_type VARCHAR(100) NOT NULL,
 file_size BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_med_attachment_record FOREIGN KEY (medical_record_id) REFERENCES medical_records(id) ON DELETE CASCADE,
 CONSTRAINT fk_med_attachment_user FOREIGN KEY (uploaded_by) REFERENCES users(id),
 INDEX idx_med_attachment_record (medical_record_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS conversation_messages (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 pet_id BIGINT NOT NULL,
 appointment_id BIGINT NULL,
 sender_id BIGINT NOT NULL,
 recipient_id BIGINT NOT NULL,
 message TEXT NOT NULL,
 attachment_name VARCHAR(255),
 attachment_url VARCHAR(500),
 attachment_type VARCHAR(100),
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 read_at DATETIME(6) NULL,
 CONSTRAINT fk_msg_pet FOREIGN KEY (pet_id) REFERENCES pets(id),
 CONSTRAINT fk_msg_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id),
 CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id),
 CONSTRAINT fk_msg_recipient FOREIGN KEY (recipient_id) REFERENCES users(id),
 INDEX idx_msg_pet_created (pet_id, created_at),
 INDEX idx_msg_sender_created (sender_id, created_at),
 INDEX idx_msg_recipient_created (recipient_id, created_at)
) ENGINE=InnoDB;
