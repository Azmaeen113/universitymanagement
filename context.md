# University Management System — Project Specification

## 1. Project Philosophy & Guidelines
- **Academic Context:** This is an undergraduate-level programming project. The code must be clean, simple, readable, and easy to explain during a lab defense.
- **Simplicity:** Use standard Spring Boot conventions. Avoid unnecessary abstractions, design patterns, microservices, reactive programming, JWT, or external frontend frameworks (No React/Angular/Vue).
- **Core Requirements:** User authentication, role-based access control, CRUD operations, database persistence, and Docker containerization.
- **Completeness:** Both the web interface (Thymeleaf) and the REST API must work completely with all validation rules enforced on the backend.

---

## 2. Technology Stack
- **Language:** Java 17
- **Framework:** Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Security, Validation)
- **Build Tool:** Maven
- **Database:** MySQL 8.0
- **Password Hashing:** `BCryptPasswordEncoder`
- **Frontend:** Thymeleaf + HTML5 + Bootstrap 5 (CDN)
- **Authentication:** Standard session-based login via Spring Security
- **Containerization:** Docker (Multi-stage Dockerfile) & Docker Compose (`compose.yaml`)

---

## 3. Database Design & Entities

Create **three separate tables / entities**. Do not merge them into a single User table.

### 1. `Admin` Table (`admins`)
- `id` (Long, PK, Auto Increment)
- `fullName` (String, required)
- `email` (String, unique, required)
- `password` (String, BCrypt hashed, required)

### 2. `Teacher` Table (`teachers`)
- `id` (Long, PK, Auto Increment)
- `fullName` (String, required)
- `email` (String, unique, required)
- `password` (String, BCrypt hashed, required)
- `department` (String, required)
- `designation` (String, required)

### 3. `Student` Table (`students`)
- `id` (Long, PK, Auto Increment)
- `fullName` (String, required)
- `email` (String, unique, required)
- `password` (String, BCrypt hashed, required)
- `department` (String, required)
- `semester` (Integer, required)
- `rollNumber` (String, unique, required)
- `approved` (Boolean, default `true` if added by Admin; default `false` if added by Teacher)

---

## 4. User Roles, Permissions & Workflow

There are three roles: `ROLE_ADMIN`, `ROLE_TEACHER`, and `ROLE_STUDENT`.

### Permission Matrix:
| Feature | Admin | Teacher | Student |
|---|---|---|---|
| Manage Admins | Full CRUD | No Access | No Access |
| Manage Teachers | Full CRUD | View all; Edit/Delete **own** profile only | No Access |
| Manage Students | Full CRUD + Approve/Reject | View all; Register new student (pending approval) | View all; Edit/Delete **own** profile only |

### Special Business Rules:
1. **Cross-Table Email Uniqueness:** An email cannot be reused across *any* of the three tables. When creating or updating an Admin, Teacher, or Student, verify against all three repositories.
2. **Roll Number Uniqueness:** Student roll numbers must be unique across all students.
3. **Self-Service / Ownership Enforcement:**
   - When a Teacher edits/deletes a teacher record, verify that `teacher.getEmail().equalsIgnoreCase(currentUserEmail)` unless the user is an Admin.
   - When a Student edits/deletes a student record, verify that `student.getEmail().equalsIgnoreCase(currentUserEmail)` unless the user is an Admin.
   - Unauthorized attempts must throw `AccessDeniedException` and return `403 Forbidden`.
4. **Teacher-to-Admin Student Approval Workflow:**
   - Teachers can register a new student via the web interface or API. The student is created with `approved = false`.
   - Admin dashboard displays a "Pending Approvals" badge with a dedicated list (`/admin/students/pending`).
   - Admin can **Approve** (sets `approved = true`) or **Reject** (deletes the record).
   - If a student tries to log in while `approved == false`, authentication must fail with the message: *"Your account is pending admin approval."*

---

## 5. Spring Security Configuration

### Login Flow:
- Form-based login at `/login` with `email` and `password` fields.
- Implement `CustomUserDetailsService` that searches for the email in `AdminRepository`, then `TeacherRepository`, and finally `StudentRepository`.
- If found in `StudentRepository` and `student.isApproved() == false`, throw `DisabledException`.
- On successful login, redirect by role:
  - `ROLE_ADMIN` ➡️ `/admin/dashboard`
  - `ROLE_TEACHER` ➡️ `/teacher/dashboard`
  - `ROLE_STUDENT` ➡️ `/student/dashboard`

### URL Authorization:
- Public: `/`, `/login`, `/css/**`, `/js/**`, `/error`
- Admin Only: `/admin/**`, `/api/admins/**`
- Admin & Teacher: `/teacher/**`, `/api/teachers/**`
- Admin & Student (plus Teacher read): `/student/**`, `/api/students/**`
- Any other request must be authenticated.

### REST API Exception Handling in Security:
- Unauthenticated API requests must return `401 Unauthorized` with JSON error body (`AuthenticationEntryPoint`).
- Forbidden API requests must return `403 Forbidden` with JSON error body (`AccessDeniedHandler`).
- Global exception handler must catch `AccessDeniedException` and return `403 Forbidden` (do NOT let Spring default to 500).

---

## 6. REST API Design

All endpoints accept and return JSON. Never return passwords in responses.

- **Admin APIs (`/api/admins`):** `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` (ADMIN only)
- **Teacher APIs (`/api/teachers`):** `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` (ADMIN & TEACHER)
- **Student APIs (`/api/students`):** `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `PUT /{id}/approve` (ADMIN, TEACHER, STUDENT)

### Standard HTTP Status Codes:
- `200 OK` — Successful GET / PUT / Approve
- `201 Created` — Successful POST
- `204 No Content` — Successful DELETE
- `400 Bad Request` — Validation failure / invalid input
- `401 Unauthorized` — Missing / invalid credentials
- `403 Forbidden` — Permission denied / editing another person's record
- `404 Not Found` — ID does not exist
- `409 Conflict` — Duplicate email or roll number

---

## 7. Web Interface (Thymeleaf + Bootstrap 5)

Provide a clean, responsive navbar and views:
1. **Login Page (`/login`)**: Email, password, error message for failed login, logout notice.
2. **Admin Views (`/admin/**`)**:
   - Dashboard with stats (counts for Admins, Teachers, Students, and Pending Approvals badge).
   - CRUD views for Admins, Teachers, and Students.
   - Pending students approval table with "Approve" and "Reject" buttons.
3. **Teacher Views (`/teacher/**`)**:
   - Dashboard showing profile info and quick links.
   - Teachers list (view-only for others; edit/delete button only on own row).
   - Students list (view-only).
   - Student registration form (shows banner: *"Student will require Admin approval before login"*).
4. **Student Views (`/student/**`)**:
   - Dashboard showing personal profile info.
   - Student directory (view other students; edit/delete button only on own profile).

---

## 8. Docker Configuration

### 1. `Dockerfile` (Multi-stage build)
- **Stage 1 (Build):** `maven:3.9-eclipse-temurin-17 AS build` ➡️ copies `pom.xml`, `src/`, runs `mvn clean package -DskipTests`.
- **Stage 2 (Runtime):** `eclipse-temurin:17-jre` ➡️ copies `app.jar` from build stage, exposes `8080`, runs `ENTRYPOINT ["java", "-jar", "app.jar"]`.

### 2. `compose.yaml`
- Set `name: university-management-system`.
- **`db` service:**
  - Image: `mysql:8.0`
  - Container name: `university_db`
  - Environment variables: `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`
  - Ports: `3306:3306`
  - Volume: `mysql_data:/var/lib/mysql`
  - **Healthcheck:** `test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]` with interval 10s, timeout 5s, retries 5.
- **`app` service:**
  - Build current directory using `Dockerfile`
  - Container name: `university_app`
  - `depends_on: db: condition: service_healthy` (Prevents Spring Boot from starting before MySQL is ready)
  - Ports: `8080:8080`
  - Spring datasource URL pointing to `jdbc:mysql://db:3306/university_db`

---



## 09. Automated Tests
Provide a full test suite using `@SpringBootTest` and `MockMvc`:
- Authentication tests (valid login, invalid login, pending student login rejected).
- Authorization tests (Teacher cannot access `/admin/**`, Student cannot access `/teacher/**`).
- Ownership tests (Teacher cannot edit another teacher's profile; Student cannot edit another student's profile).
- Cross-table uniqueness test (Reject duplicate email across different tables).