USE pawcare_db;
-- Additions for role-specific registration and persisted contact forms.
CREATE TABLE IF NOT EXISTS stakeholder_profiles (
 user_id BIGINT PRIMARY KEY, organization VARCHAR(150), license_number VARCHAR(100), qualification VARCHAR(200),
 experience_years INT, specialty VARCHAR(150), business_registration VARCHAR(100), employee_id VARCHAR(100), department VARCHAR(100),
 CONSTRAINT fk_stakeholder_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT chk_experience CHECK (experience_years IS NULL OR experience_years BETWEEN 0 AND 60)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS contact_messages (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, email VARCHAR(150) NOT NULL,
 subject VARCHAR(200) NOT NULL, message VARCHAR(2000) NOT NULL, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;
