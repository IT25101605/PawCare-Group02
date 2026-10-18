-- PawCare Sample Data
USE pawcare_db;

-- Roles (7 roles for 7 stakeholders)
INSERT INTO roles (name, description) VALUES
('ADMIN', 'System Administrator'),
('PET_OWNER', 'Pet Owner'),
('VETERINARIAN', 'Veterinary Doctor'),
('GROOMER', 'Pet Groomer'),
('BOARDING_STAFF', 'Boarding Facility Staff'),
('STORE_MANAGER', 'Pet Store Manager'),
('SERVICE_STAFF', 'Payment & Service Staff');

-- Users (password is BCrypt of 'Password123!')
INSERT INTO users (first_name, last_name, email, password, phone, address, city, state, zip_code, enabled, email_verified, role_id) VALUES
('Admin', 'User', 'admin@pawcare.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0710000100', '123 Admin St', 'New York', 'NY', '10001', TRUE, TRUE, 1),
('John', 'Smith', 'john@pawcare.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0710000101', '456 Pet Ave', 'Los Angeles', 'CA', '90001', TRUE, TRUE, 2),
('Sarah', 'Johnson', 'sarah@pawcare.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0710000102', '789 Owner Ln', 'Chicago', 'IL', '60601', TRUE, TRUE, 2),
('Dr. Emily', 'Brown', 'emily@pawcare.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0710000103', '321 Vet Blvd', 'Houston', 'TX', '77001', TRUE, TRUE, 3),
('Dr. Michael', 'Davis', 'michael@pawcare.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0710000104', '654 Medical Dr', 'Phoenix', 'AZ', '85001', TRUE, TRUE, 3),
('Lisa', 'Wilson', 'lisa@pawcare.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0710000105', '987 Groom St', 'Dallas', 'TX', '75201', TRUE, TRUE, 4),
('Tom', 'Anderson', 'tom@pawcare.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0710000106', '147 Board Ave', 'Miami', 'FL', '33101', TRUE, TRUE, 5),
('Karen', 'Martinez', 'karen@pawcare.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0710000107', '258 Store Rd', 'Seattle', 'WA', '98101', TRUE, TRUE, 6),
('David', 'Clark', 'david@pawcare.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '0710000108', '369 Finance Ave', 'Boston', 'MA', '02101', TRUE, TRUE, 7);

-- Species
INSERT INTO species (name, description) VALUES
('Dog', 'Domestic dogs'), ('Cat', 'Domestic cats'), ('Bird', 'Pet birds'),
('Rabbit', 'Domestic rabbits'), ('Fish', 'Aquarium fish'), ('Hamster', 'Pet hamsters');

-- Breeds
INSERT INTO breeds (name, species_id, description) VALUES
('Labrador Retriever', 1, 'Friendly and active'), ('German Shepherd', 1, 'Loyal and intelligent'),
('Golden Retriever', 1, 'Gentle and friendly'), ('Bulldog', 1, 'Calm and courageous'),
('Poodle', 1, 'Intelligent and elegant'), ('Beagle', 1, 'Curious and friendly'),
('Persian', 2, 'Long-haired and calm'), ('Siamese', 2, 'Social and vocal'),
('Maine Coon', 2, 'Large and gentle'), ('British Shorthair', 2, 'Easy-going'),
('Cockatiel', 3, 'Friendly and social'), ('Budgerigar', 3, 'Small and colorful'),
('Holland Lop', 4, 'Friendly rabbit breed'), ('Syrian', 6, 'Common pet hamster');

-- Pets
INSERT INTO pets (name, species_id, breed_id, date_of_birth, gender, weight, color, owner_id) VALUES
('Buddy', 1, 1, '2021-03-15', 'MALE', 30.5, 'Golden', 2),
('Luna', 2, 7, '2022-06-20', 'FEMALE', 4.2, 'White', 2),
('Max', 1, 2, '2020-01-10', 'MALE', 35.0, 'Black/Tan', 3),
('Bella', 2, 8, '2021-11-05', 'FEMALE', 3.8, 'Cream', 3),
('Charlie', 1, 3, '2022-08-22', 'MALE', 28.0, 'Golden', 2);

-- Grooming Services
INSERT INTO grooming_services (name, description, duration_minutes, price) VALUES
('Basic Bath', 'Shampoo bath, blow dry and brushing', 45, 2800.00),
('Full Grooming', 'Full-body trim with bath, brushing and finishing care', 90, 6000.00),
('Nail Trimming', 'Careful nail clipping and paw check', 15, 500.00),
('Ear Cleaning', 'Gentle external ear cleaning', 15, 500.00),
('Teeth Cleaning', 'Basic teeth cleaning and mouth freshening', 15, 450.00),
('Hygiene Trim', 'Sanitary, paw-pad and eye-area hygiene trim', 30, 1500.00),
('De-shedding Treatment', 'Specialized de-shedding shampoo and brush-out', 45, 1800.00),
('Flea & Tick Treatment', 'Medicated bath and coat care for flea/tick treatment', 45, 2200.00);

-- Boarding Facilities
INSERT INTO boarding_facilities (name, facility_type, capacity, daily_rate, description, amenities) VALUES
('Standard Kennel', 'STANDARD', 20, 35.00, 'Clean comfortable kennel with outdoor access', 'Food, Water, Daily Walk'),
('Premium Suite', 'PREMIUM', 10, 65.00, 'Spacious private suite with bed and toys', 'Premium Food, Playtime, Webcam, Bed'),
('Luxury Villa', 'LUXURY', 5, 120.00, 'Private villa with personal caretaker', 'Gourmet Meals, Spa, Personal Caretaker, TV');

-- Product Categories
INSERT INTO product_categories (name, description) VALUES
('Dog Food', 'Premium dog food and treats'),
('Cat Food', 'Premium cat food and treats'),
('Toys', 'Pet toys and entertainment'),
('Accessories', 'Collars, leashes, and accessories'),
('Health & Wellness', 'Vitamins, supplements and health products'),
('Grooming Supplies', 'Shampoos, brushes and grooming tools');

-- Products
INSERT INTO products (name, description, price, sale_price, sku, stock_quantity, category_id, brand, is_featured) VALUES
('Premium Dog Kibble 10kg', 'High-quality dry dog food with real chicken', 7500.00, 6900.00, 'DOG-FOOD-001', 100, 1, 'PawNutrition', TRUE),
('Grain-Free Cat Food 5kg', 'Natural grain-free formula for cats', 5200.00, NULL, 'CAT-FOOD-001', 80, 2, 'FelineFeast', TRUE),
('Interactive Ball Launcher', 'Automatic ball launcher for dogs', 3500.00, 3200.00, 'TOY-001', 30, 3, 'PlayPets', TRUE),
('Leather Dog Collar - Medium', 'Genuine leather adjustable collar', 2200.00, NULL, 'ACC-001', 50, 4, 'PetStyle', FALSE),
('Multivitamin Chews for Dogs', 'Daily vitamin supplement for dogs', 2800.00, 2500.00, 'HEALTH-001', 60, 5, 'VitaPaws', TRUE),
('Deshedding Brush', 'Professional grade deshedding tool', 1800.00, NULL, 'GROOM-001', 45, 6, 'GroomPro', FALSE),
('Salmon Dog Treats 500g', 'Natural salmon training treats', 2400.00, 2100.00, 'DOG-FOOD-002', 120, 1, 'PawNutrition', FALSE),
('Catnip Mouse Toy', 'Interactive catnip-filled mouse toy', 850.00, NULL, 'TOY-002', 200, 3, 'PlayPets', FALSE),
('GPS Pet Tracker', 'Real-time GPS tracking collar attachment', 9500.00, 8900.00, 'ACC-002', 25, 4, 'PetTech', TRUE),
('Organic Pet Shampoo 500ml', 'All-natural organic pet shampoo', 1900.00, NULL, 'GROOM-002', 70, 6, 'GroomPro', FALSE);

-- Appointments
INSERT INTO appointments (pet_id, owner_id, veterinarian_id, appointment_date, appointment_time, reason, status) VALUES
(1, 2, 4, '2026-08-25', '09:00:00', 'Annual checkup and vaccination', 'SCHEDULED'),
(2, 2, 4, '2026-08-25', '10:00:00', 'Dental examination', 'SCHEDULED'),
(3, 3, 5, '2026-08-26', '14:00:00', 'Skin allergy follow-up', 'CONFIRMED'),
(4, 3, 4, '2026-08-27', '11:00:00', 'Vaccination booster', 'SCHEDULED'),
(5, 2, 5, '2026-08-20', '09:30:00', 'General wellness check', 'COMPLETED');

-- Medical Records
INSERT INTO medical_records (pet_id, veterinarian_id, appointment_id, diagnosis, treatment, prescription, weight_at_visit, temperature, record_date) VALUES
(5, 5, 5, 'Healthy - no issues found', 'General wellness examination completed', 'None required', 28.0, 38.5, '2026-08-20'),
(1, 4, NULL, 'Mild ear infection', 'Ear cleaning and antibiotic drops', 'Otibiotic Ointment - 2 drops twice daily for 7 days', 30.5, 38.8, '2026-08-15');

-- Vaccinations
INSERT INTO vaccinations (pet_id, vaccine_name, date_administered, next_due_date, administered_by) VALUES
(1, 'Rabies', '2026-01-15', '2027-01-15', 4),
(1, 'DHPP', '2026-01-15', '2027-01-15', 4),
(2, 'FVRCP', '2026-02-20', '2027-02-20', 4),
(3, 'Rabies', '2025-12-10', '2026-12-10', 5),
(5, 'Rabies', '2026-07-25', '2027-07-25', 5);

-- Notifications
INSERT INTO notifications (user_id, title, message, notification_type) VALUES
(2, 'Welcome to PawCare!', 'Thank you for joining PawCare. Start by adding your pets.', 'SUCCESS'),
(2, 'Upcoming Appointment', 'Buddy has an appointment on Aug 25th at 9:00 AM.', 'APPOINTMENT'),
(3, 'Appointment Confirmed', 'Max appointment on Aug 26th has been confirmed.', 'APPOINTMENT'),
(4, 'New Appointment', 'You have a new appointment scheduled for Aug 25th.', 'INFO');
