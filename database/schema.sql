-- PawCare Database Schema - MySQL 8
CREATE DATABASE IF NOT EXISTS pawcare_db;
USE pawcare_db;

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    profile_image VARCHAR(500),
    enabled BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id),
    INDEX idx_users_email (email),
    INDEX idx_users_role (role_id)
) ENGINE=InnoDB;

CREATE TABLE verification_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    token_type VARCHAR(50) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_tokens_token (token)
) ENGINE=InnoDB;

CREATE TABLE species (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE breeds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    species_id BIGINT NOT NULL,
    description VARCHAR(255),
    FOREIGN KEY (species_id) REFERENCES species(id),
    INDEX idx_breeds_species (species_id)
) ENGINE=InnoDB;

CREATE TABLE pets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    species_id BIGINT NOT NULL,
    breed_id BIGINT,
    date_of_birth DATE,
    gender ENUM('MALE','FEMALE','UNKNOWN') DEFAULT 'UNKNOWN',
    weight DECIMAL(5,2),
    color VARCHAR(50),
    microchip_id VARCHAR(50),
    photo VARCHAR(500),
    notes TEXT,
    owner_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (species_id) REFERENCES species(id),
    FOREIGN KEY (breed_id) REFERENCES breeds(id),
    FOREIGN KEY (owner_id) REFERENCES users(id),
    INDEX idx_pets_owner (owner_id)
) ENGINE=InnoDB;

CREATE TABLE vaccinations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    vaccine_name VARCHAR(200) NOT NULL,
    date_administered DATE NOT NULL,
    next_due_date DATE,
    administered_by BIGINT,
    batch_number VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE,
    FOREIGN KEY (administered_by) REFERENCES users(id),
    INDEX idx_vaccinations_pet (pet_id)
) ENGINE=InnoDB;

CREATE TABLE appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    veterinarian_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status ENUM('SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW') DEFAULT 'SCHEDULED',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id),
    FOREIGN KEY (owner_id) REFERENCES users(id),
    FOREIGN KEY (veterinarian_id) REFERENCES users(id),
    INDEX idx_appt_date (appointment_date),
    INDEX idx_appt_status (status)
) ENGINE=InnoDB;

CREATE TABLE medical_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    veterinarian_id BIGINT NOT NULL,
    appointment_id BIGINT,
    diagnosis VARCHAR(500) NOT NULL,
    treatment TEXT NOT NULL,
    prescription TEXT,
    weight_at_visit DECIMAL(5,2),
    temperature DECIMAL(4,1),
    follow_up_date DATE,
    record_date DATE NOT NULL,
    record_type VARCHAR(30),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id),
    FOREIGN KEY (veterinarian_id) REFERENCES users(id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    INDEX idx_med_pet (pet_id)
) ENGINE=InnoDB;

CREATE TABLE grooming_services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    duration_minutes INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE grooming_appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    groomer_id BIGINT,
    service_id BIGINT NOT NULL,
    selected_service_ids VARCHAR(1000),
    selected_service_names VARCHAR(2000),
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status ENUM('SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'SCHEDULED',
    special_instructions TEXT,
    comfort_profile VARCHAR(20),
    preferred_finish VARCHAR(30),
    completed_tasks VARCHAR(250),
    products_used VARCHAR(500),
    next_visit_date DATE,
    total_price DECIMAL(10,2) NOT NULL,
    total_duration_minutes INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id),
    FOREIGN KEY (owner_id) REFERENCES users(id),
    FOREIGN KEY (groomer_id) REFERENCES users(id),
    FOREIGN KEY (service_id) REFERENCES grooming_services(id),
    INDEX idx_groom_date (appointment_date)
) ENGINE=InnoDB;

CREATE TABLE grooming_appointment_services (
    grooming_appointment_id BIGINT NOT NULL,
    grooming_service_id BIGINT NOT NULL,
    PRIMARY KEY (grooming_appointment_id, grooming_service_id),
    CONSTRAINT fk_groom_appt_services_appt FOREIGN KEY (grooming_appointment_id) REFERENCES grooming_appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_groom_appt_services_service FOREIGN KEY (grooming_service_id) REFERENCES grooming_services(id),
    INDEX idx_groom_appt_services_service (grooming_service_id)
) ENGINE=InnoDB;

CREATE TABLE boarding_facilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    facility_type ENUM('STANDARD','PREMIUM','LUXURY') NOT NULL,
    capacity INT NOT NULL,
    daily_rate DECIMAL(10,2) NOT NULL,
    description TEXT,
    amenities TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE boarding_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    staff_id BIGINT,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    status ENUM('RESERVED','CHECKED_IN','CHECKED_OUT','CANCELLED') DEFAULT 'RESERVED',
    special_requirements TEXT,
    feeding_instructions TEXT,
    medication_instructions TEXT,
    emergency_contact VARCHAR(100),
    total_cost DECIMAL(10,2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id),
    FOREIGN KEY (owner_id) REFERENCES users(id),
    FOREIGN KEY (facility_id) REFERENCES boarding_facilities(id),
    FOREIGN KEY (staff_id) REFERENCES users(id),
    INDEX idx_board_dates (check_in_date, check_out_date)
) ENGINE=InnoDB;

CREATE TABLE product_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    image VARCHAR(500),
    parent_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES product_categories(id)
) ENGINE=InnoDB;

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    sale_price DECIMAL(10,2),
    sku VARCHAR(50) UNIQUE,
    stock_quantity INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 0,
    category_id BIGINT NOT NULL,
    image VARCHAR(500),
    brand VARCHAR(100),
    weight VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    is_featured BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES product_categories(id),
    INDEX idx_prod_category (category_id),
    INDEX idx_prod_sku (sku)
) ENGINE=InnoDB;

CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_ci_cart (cart_id)
) ENGINE=InnoDB;

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) DEFAULT 0.00,
    shipping_cost DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED','REFUNDED') DEFAULT 'PENDING',
    shipping_address TEXT,
    billing_address TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_ord_user (user_id),
    INDEX idx_ord_status (status)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_oi_order (order_id)
) ENGINE=InnoDB;

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM('CREDIT_CARD','DEBIT_CARD','PAYPAL','BANK_TRANSFER','CASH','SIMULATED') NOT NULL,
    payment_status ENUM('PENDING','COMPLETED','FAILED','REFUNDED') DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(500),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_pay_order (order_id)
) ENGINE=InnoDB;

CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) DEFAULT 0.00,
    total DECIMAL(10,2) NOT NULL,
    status ENUM('DRAFT','SENT','PAID','OVERDUE','CANCELLED') DEFAULT 'DRAFT',
    due_date DATE,
    paid_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT,
    order_id BIGINT,
    grooming_appointment_id BIGINT,
    grooming_service_id BIGINT,
    groomer_id BIGINT,
    pet_id BIGINT,
    rating INT NOT NULL,
    comment TEXT,
    review_type ENUM('PRODUCT','SERVICE','VET','GROOMING','BOARDING') NOT NULL,
    is_approved BOOLEAN DEFAULT TRUE,
    moderation_status ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'APPROVED',
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    is_reported BOOLEAN NOT NULL DEFAULT FALSE,
    report_reason VARCHAR(500),
    reported_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    moderated_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (grooming_appointment_id) REFERENCES grooming_appointments(id),
    FOREIGN KEY (grooming_service_id) REFERENCES grooming_services(id),
    FOREIGN KEY (groomer_id) REFERENCES users(id),
    FOREIGN KEY (pet_id) REFERENCES pets(id),
    UNIQUE KEY uk_review_service_once (user_id, grooming_appointment_id, review_type),
    UNIQUE KEY uk_review_product_order_once (user_id, product_id, order_id, review_type),
    INDEX idx_review_product_public (product_id, moderation_status, is_hidden, created_at),
    INDEX idx_review_moderation (moderation_status, is_reported, created_at)
) ENGINE=InnoDB;

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type ENUM('INFO','WARNING','SUCCESS','ERROR','APPOINTMENT','ORDER','PAYMENT','SYSTEM') DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    link VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_audit_date (created_at)
) ENGINE=InnoDB;

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

-- PawCare medical advanced additions (2026-09-20)
CREATE TABLE IF NOT EXISTS medical_attachments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, medical_record_id BIGINT NOT NULL, uploaded_by BIGINT NOT NULL,
 file_name VARCHAR(255) NOT NULL, file_url VARCHAR(500) NOT NULL, content_type VARCHAR(100) NOT NULL,
 file_size BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (medical_record_id) REFERENCES medical_records(id) ON DELETE CASCADE,
 FOREIGN KEY (uploaded_by) REFERENCES users(id), INDEX idx_med_attachment_record (medical_record_id, created_at)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS message_conversations (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 pet_id BIGINT NOT NULL, owner_id BIGINT NOT NULL, veterinarian_id BIGINT NOT NULL, latest_appointment_id BIGINT,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, latest_message_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (pet_id) REFERENCES pets(id), FOREIGN KEY (owner_id) REFERENCES users(id),
 FOREIGN KEY (veterinarian_id) REFERENCES users(id), FOREIGN KEY (latest_appointment_id) REFERENCES appointments(id),
 UNIQUE KEY uk_message_conversation_pair (pet_id, owner_id, veterinarian_id),
 INDEX idx_message_conversation_latest (latest_message_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS conversation_messages (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, conversation_id BIGINT, pet_id BIGINT NOT NULL, appointment_id BIGINT, sender_id BIGINT NOT NULL,
 recipient_id BIGINT NOT NULL, message TEXT NOT NULL, attachment_name VARCHAR(255), attachment_url VARCHAR(500),
 attachment_type VARCHAR(100), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, read_at TIMESTAMP NULL,
 FOREIGN KEY (conversation_id) REFERENCES message_conversations(id), FOREIGN KEY (pet_id) REFERENCES pets(id),
 FOREIGN KEY (appointment_id) REFERENCES appointments(id), FOREIGN KEY (sender_id) REFERENCES users(id),
 FOREIGN KEY (recipient_id) REFERENCES users(id),
 INDEX idx_msg_conversation_created (conversation_id, created_at), INDEX idx_msg_pet_created (pet_id, created_at),
 INDEX idx_msg_sender_created (sender_id, created_at), INDEX idx_msg_recipient_created (recipient_id, created_at)
) ENGINE=InnoDB;
