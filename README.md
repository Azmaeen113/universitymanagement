# University Management System

Spring Boot 3 (Java 17) + Spring Security + Spring Data JPA + MySQL 8 + Thymeleaf + Bootstrap 5.
Three roles: **Admin**, **Teacher**, **Student**. Web pages and a REST API.

## Run with Docker (recommended)

```bash
docker compose up --build
```

Open <http://localhost:8080>. Stop with `docker compose down` (add `-v` to also delete the database).

## Run without Docker

1. Start MySQL 8 and create database `university_db` with user `university_user` / password `university_pass`
   (or change `src/main/resources/application.properties`).
2. `mvn spring-boot:run`

## Run the tests (no MySQL needed, they use in-memory H2)

```bash
mvn test
```

## Tests and CI

- 6 unit tests (`src/test/java/.../unit`, JUnit + Mockito, no Spring) and 3 end-to-end integration tests
  (`EndToEndIntegrationTest`). Run them with `mvn test`.
- GitHub Actions (`.github/workflows/ci.yml`) runs the unit tests, then the integration tests on every
  push and pull request.

## Test accounts (created automatically on first start)

| Role    | Email                          | Password     |
|---------|--------------------------------|--------------|
| Admin   | admin@university.edu           | admin123     |
| Admin   | rahim.admin@university.edu     | admin123     |
| Teacher | sarah.khan@university.edu      | teacher123   |
| Teacher | imran.hossain@university.edu   | teacher123   |
| Teacher | nusrat.jahan@university.edu    | teacher123   |
| Student | ayesha.rahman@student.edu      | student123   |
| Student | tanvir.ahmed@student.edu       | student123   |
| Student | maliha.chowdhury@student.edu   | student123   |
| Student (pending, cannot log in) | sakib.hasan@student.edu  | student123 |
| Student (pending, cannot log in) | nadia.islam@student.edu  | student123 |

## REST API quick try (HTTP Basic with email + password)

```bash
curl -u admin@university.edu:admin123 http://localhost:8080/api/students
curl -u admin@university.edu:admin123 -X PUT http://localhost:8080/api/students/4/approve
```

| Endpoint | Who |
|---|---|
| `/api/admins` (POST, GET, GET/{id}, PUT/{id}, DELETE/{id}) | Admin |
| `/api/teachers` (same 5) | Admin, Teacher (create = Admin only; edit/delete = Admin or the teacher himself) |
| `/api/students` (same 5) + `PUT /api/students/{id}/approve` | Admin, Teacher (list + register), Student (list + own profile); approve = Admin only |

Status codes: 200, 201, 204, 400, 401, 403, 404, 409.

Detailed Bangla explanation for the viva: [EXPLANATION_BANGLA.md](EXPLANATION_BANGLA.md)

## Postman demo

Ready-to-import Postman files are in the `postman/` folder:

- `University-Management.postman_collection.json`
- `University-Management.postman_environment.json`

In Postman, import both files and select the **University Management - Local Docker**
environment. Start the application with `docker compose up -d`, then run the collection
folders in order:

1. Authentication and authorization
2. Admin operations
3. Teacher registers pending student
4. Student approval
5. Validation and duplicate handling

The collection uses HTTP Basic Auth and `http://localhost:8080` by default. The
`pendingStudentId` environment variable is set to `4`, matching the seeded pending
student in a fresh database. If the ID is different in your database, update that
variable before running the approval requests.
