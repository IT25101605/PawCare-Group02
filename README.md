# PawCare — Web-Based Pet Care Management System 🐾

**SE2030 Software Engineering | Year 2, Semester 1 (2026)**  
**Sri Lanka Institute of Information Technology (SLIIT) | Group 02**

## Project Overview

**PawCare** is a web-based pet care management system that brings essential pet services into one application. Pet owners can manage pet profiles, book veterinary and grooming appointments, view medical information, purchase pet supplies, and keep track of payments and notifications. Role-specific workspaces help veterinarians, groomers, store staff, service staff, and administrators carry out their responsibilities.

The project is developed for the SE2030 Software Engineering module. It uses a Java Spring Boot backend, a MySQL database, and a responsive HTML/CSS/JavaScript frontend.

## Main Features

| Module | Features |
| --- | --- |
| **1. User Authentication & Management** | Registration, login, profile management, password management, role-based access, and administrator user management. |
| **2. Pet Profile & Pet Management** | Add, view, update, and delete pet profiles; manage pet details and digital pet passports with QR codes. |
| **3. Veterinary Appointment Management** | Book, view, reschedule, and cancel appointments; manage veterinarian availability and appointment status. |
| **4. Medical Records & Health Management** | Record diagnoses, treatments, prescriptions, and vaccinations; view medical histories and manage medical attachments. |
| **5. Grooming Service Management** | Browse grooming services, book available slots, manage groomer schedules, and update booking and service status. |
| **6. Pet Store & Order Management** | Browse products, manage shopping carts, place orders, track order status, and manage products and inventory. |
| **7. Payment, Notification & Reporting** | Manage payment transactions and receipts, deliver system notifications, and display operational reports and dashboards. |

### Additional Capabilities

- **PawCare Assistant:** Context-aware information about recorded pet-care activities, application navigation, and general pet-care questions. It is **not a veterinary diagnosis tool**.
- **Digital Pet Passport:** Selectively shared pet details and QR-code access.
- **Ratings and reviews** for supported services and products.
- **Secure messaging** and notifications within supported application workflows.
- **Demo data initialization** for local testing and presentations.

## Technologies Used

| Layer | Technologies |
| --- | --- |
| Backend | Java 21, Spring Boot 3.4.1, Spring Web, Spring Data JPA, Hibernate |
| Authentication | Spring Security, JSON Web Tokens (JWT), BCrypt password hashing |
| Frontend | HTML5, CSS3, JavaScript, Bootstrap 5, Bootstrap Icons |
| Database | MySQL 8 |
| Build | Maven and Maven Wrapper |
| API documentation | Springdoc OpenAPI / Swagger UI |
| Testing | JUnit / Spring Boot Test, Spring Security Test |

## System Architecture

PawCare follows a layered Spring Boot application structure:

```text
Frontend (HTML / CSS / JavaScript)
           |
           v
REST Controllers -> Services -> JPA Repositories -> MySQL
           |
   Authentication / Authorization (Spring Security + JWT)
```

## Prerequisites

Install the following before running the project:

- **JDK 21**
- **MySQL 8** (running locally or reachable from your computer)
- **IntelliJ IDEA** (recommended) or another Java IDE
- Internet access for the initial Maven dependency download

The Maven Wrapper is included, so a separate global Maven installation is not necessary.

## Installation and Running the Project

### 1. Open the project

Clone the repository, or download it as a ZIP and extract it. Open the **`PawCare`** folder containing `pom.xml` in IntelliJ IDEA. Allow Maven to load dependencies and select **JDK 21** as the Project SDK.

### 2. Configure MySQL

Start MySQL. By default, PawCare connects to:

```text
Database: pawcare_db
Host: localhost
Port: 3306
Username: root
```

The default JDBC URL can create `pawcare_db` automatically if your MySQL account has permission. Alternatively, create it manually:

```sql
CREATE DATABASE IF NOT EXISTS pawcare_db;
```

**Do not commit your MySQL password to GitHub.** The application reads its password from the `SPRING_DATASOURCE_PASSWORD` environment variable.

### 3. Run with Maven

**Windows (PowerShell):**

```powershell
$env:SPRING_DATASOURCE_PASSWORD = 'YOUR_MYSQL_PASSWORD'
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**

```bash
export SPRING_DATASOURCE_PASSWORD='YOUR_MYSQL_PASSWORD'
chmod +x mvnw
./mvnw spring-boot:run
```

If needed, set `SPRING_DATASOURCE_USERNAME` or `SPRING_DATASOURCE_URL` to override the default username or connection URL.

**Run directly in IntelliJ:** Open `src/main/java/com/pawcare/demo/PawCareApplication.java`, select **Run**, and set `SPRING_DATASOURCE_PASSWORD=YOUR_MYSQL_PASSWORD` under **Run > Edit Configurations > Environment variables** before launching.

### 4. Open the application

Once the server starts, visit:

- **Application:** http://localhost:8080/
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI specification:** http://localhost:8080/api-docs

Stop the application with **Ctrl + C** in the terminal or the **Stop** button in IntelliJ.

## Database and Demonstration Data

On a fresh database, the application uses Hibernate's schema update configuration and built-in data seeders to initialize required data, including demonstration roles/accounts and catalog content.

Database reference files are included in:

```text
database/
├── schema.sql
├── sample-data.sql
└── migrations/
```

**Important:** The sample SQL file contains legacy example IDs, and the migrations include historical changes. Do **not** run all of them blindly on a new or existing database. The application seeders are the normal starting point for a fresh local setup. Existing MySQL records are **not** stored in the GitHub source repository.

Demo users are defined in `src/main/java/com/pawcare/demo/config/DataSeeder.java`. Seeded accounts are for **local demonstration only**; change their default passwords before using PawCare in a publicly accessible environment.

## Build and Test

Run these commands from the project root:

**Windows:**

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd package
```

**macOS / Linux:**

```bash
./mvnw clean test
./mvnw package
```

The packaged JAR is generated in `target/` after a successful Maven build. Tests may require the appropriate local test/database configuration.

Optional frontend JavaScript syntax check (requires Node.js):

```bash
node check-frontend.cjs
```

## Design Patterns Implemented

The project includes seven design-pattern implementations across the group modules:

| Module | Design Pattern | Reference in Source Code |
| --- | --- | --- |
| User Authentication & Management | Factory Method | `registration/factory/StakeholderProfileFactory.java` |
| Pet Profile & Management | Builder | `pet/builder/PetRegistrationBuilder.java` |
| Veterinary Appointment Management | Chain of Responsibility | `appointment/chain/AppointmentBookingValidationChain.java` |
| Medical Records & Health Management | Specification | `medical/specification/MedicalRecordSpecifications.java` |
| Grooming Service Management | State | `grooming/state/GroomingStateContext.java` |
| Pet Store & Order Management | Facade | `store/facade/StoreCheckoutFacade.java` |
| Payment, Notification & Reporting | Strategy | `notification/strategy/NotificationStrategy.java` |

Paths above are relative to `src/main/java/com/pawcare/demo/`.

## Project Structure

```text
PawCare/
├── .mvn/                          # Maven Wrapper configuration
├── database/                      # SQL references and migrations
├── src/
│   ├── main/
│   │   ├── java/com/pawcare/demo/ # Controllers, services, entities,
│   │   │                          # repositories, security and patterns
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/            # Web pages, JS, CSS, images and media
│   └── test/java/                 # Automated tests
├── tools/                         # Local startup helper
├── .gitignore
├── check-frontend.cjs
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Group 02 — Team Members and Contributions

| Student ID | Team Member | Assigned Module |
| --- | --- | --- |
| IT25103148 | PRIYADARSHANI H.A.P | User Authentication and User Management |
| IT25102681 | WEESINGHE W.M.D.S | Pet Profile and Pet Management |
| IT25101057 | WEERASINGHE S.M.S | Veterinary Appointment Management |
| IT25101013 | WIJETHUNGA W.M.H.S | Medical Records and Health Management |
| IT25101196 | GUNATHUNGA H.C.J | Grooming Service Management |
| IT25101605 | RATHNAYAKA R.M.J.N | Pet Store and Order Management |
| IT25102608 | SENARATHNA S.K.T.B | Payment, Notification and Reporting Management |

## Notes and Limitations

- Some payment operations are **simulated for academic demonstration** rather than processed through a live payment gateway.
- Email functionality requires valid SMTP credentials and configuration.
- The PawCare Assistant provides record summaries and general guidance; it does not replace professional veterinary assessment.
- The repository intentionally excludes private runtime uploads, production secrets, build output, and personal database contents.
- The included configuration has a development JWT fallback. Set a unique `APP_JWT_SECRET`, replace demo credentials, and review application security before any public deployment.

