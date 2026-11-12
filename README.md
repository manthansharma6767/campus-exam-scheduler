# Campus Exam Scheduler

Campus Exam Scheduler is a full-stack application for publishing university examination timetables and managing the data behind them. Administrators maintain departments, subjects, students, exams, absentee-paper eligibility, and seating plans. Students can search schedules by enrollment number, browse published exams, keep a personal schedule in the browser, export it as an iCalendar file, and manage absentee papers after verifying their date of birth.

> **Project status:** active development. The current implementation includes hardened student verification and absentee-assignment controls. Review [Known limitations](#known-limitations) before using it for a production examination cycle.

## Features

### Administration

- One-time, token-protected bootstrap for the first administrator
- Stateless JWT authentication for the administration area
- CRUD management for departments, subjects, students, exams, and administrators
- Student roster import from CSV, XLS, and XLSX with previews, row-level errors, duplicate skipping, and atomic saves
- Administrator DOB backfill from CSV, XLS, or XLSX using enrollment numbers, with preview and protection against overwriting existing dates
- Timetable import from XLS, XLSX, text-based PDF, and scanned/image-only PDF via OCR
- Import previews with validation errors, duplicate counts, and room/cohort conflict details
- Separate `REGULAR` and `ABSENTEE` exam types
- Per-student absentee-paper assignments with administrator-controlled locks
- Seating-plan PDF import and public roll-number seat lookup
- Administrator password change from the dashboard

### Student experience

- Public schedule search by enrollment number
- Browse and filter published exams by query, semester, date, and exam type
- Save selected exams in the browser and export them as an `.ics` calendar
- Verify enrollment number plus date of birth to manage personal absentee-paper eligibility
- View assigned absentee papers in place of the corresponding regular paper

### Platform and operations

- Flyway-owned schema migrations with Hibernate schema validation
- Bean Validation, consistent JSON error responses, and a 10 MB upload limit
- OpenAPI documentation through Swagger UI
- Backend integration and import tests plus frontend lint and production build checks

## Technology

| Layer | Technologies |
| --- | --- |
| Backend | Java 17, Spring Boot 4.1.0, Spring Security, Spring Data JPA, Bean Validation, Lombok |
| Authentication | JJWT 0.12.7, BCrypt, stateless JWT |
| Database | MySQL 8.0+, Flyway |
| File processing | Apache POI 5.5.1, Apache PDFBox 3.0.8, Tesseract OCR 5+ (optional host dependency) |
| Frontend | React 19.2, Vite 8.2, Material UI 9, React Router 7, Axios 1.19 |
| Quality | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test, oxlint |
| API documentation | Springdoc OpenAPI 2.8.9 / Swagger UI |

## Repository layout

```text
.
|-- backend/
|   |-- src/main/java/com/manthan/campusexamscheduler/
|   |   |-- config/          # security, CORS, OpenAPI
|   |   |-- controller/      # REST controllers
|   |   |-- service/         # business logic, imports, verification
|   |   |-- repository/      # Spring Data repositories
|   |   `-- entity/          # JPA entities
|   |-- src/main/resources/db/migration/   # Flyway V1 and V2
|   |-- src/test/java/...                   # integration and import tests
|   |-- Dockerfile
|   `-- mvnw, mvnw.cmd
|-- frontend/                # React/Vite single-page application
|-- database/
|   |-- migrations/          # Legacy manual SQL snippets; Flyway is authoritative
|   `-- demo-data.sql        # Optional synthetic demo data
|-- postman/ and .postman/   # Optional Postman workspace metadata
|-- .env.example             # Environment-variable reference
`-- README.md
```

## Prerequisites

- JDK 17 or newer
- Node.js 20.19+ or 22.12+ and npm (the Node requirement comes from Vite 8)
- MySQL 8.0 or newer
- Git
- Tesseract OCR 5+ with language data, only if scanned timetable PDFs will be imported
- Docker, only if building or running the backend container

## Local setup

### 1. Clone the repository

```bash
git clone https://github.com/manthansharma6767/campus-exam-scheduler.git
cd campus-exam-scheduler
```

### 2. Create the MySQL database and application user

Run the following as a MySQL administrator. Replace the password placeholder with a strong local password.

```sql
CREATE DATABASE campus_exam_scheduler
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'campus_app'@'localhost' IDENTIFIED BY 'replace-with-a-strong-password';
GRANT ALL PRIVILEGES ON campus_exam_scheduler.* TO 'campus_app'@'localhost';
FLUSH PRIVILEGES;
```

Flyway needs permission to create and alter objects in this schema. Do not reuse production credentials for local development.

### 3. Start the backend

The backend requires an explicit database connection and JWT secret. There are no fallback database credentials or JWT secret in the source configuration. Set the variables in your shell, IDE run configuration, or deployment secret store. For local development, PowerShell is shown first and the equivalent POSIX commands follow.

```powershell
Set-Location backend
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/campus_exam_scheduler?serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME = "campus_app"
$env:SPRING_DATASOURCE_PASSWORD = "replace-with-a-strong-password"
$env:JWT_SECRET = "replace-with-a-random-secret-at-least-32-bytes"
$env:ADMIN_SETUP_TOKEN = "replace-with-a-random-one-time-token"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173,http://localhost:5174"
.\mvnw.cmd spring-boot:run
```

```bash
cd backend
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/campus_exam_scheduler?serverTimezone=UTC"
export SPRING_DATASOURCE_USERNAME="campus_app"
export SPRING_DATASOURCE_PASSWORD="replace-with-a-strong-password"
export JWT_SECRET="replace-with-a-random-secret-at-least-32-bytes"
export ADMIN_SETUP_TOKEN="replace-with-a-random-one-time-token"
export CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:5174"
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`. On startup, Flyway applies pending migrations and Hibernate validates the resulting schema. Hibernate does not create or update tables.

Keep `JWT_SECRET` and `ADMIN_SETUP_TOKEN` out of source control. Use a cryptographically random value for each; the JWT secret must be at least 32 bytes for the HS256 signing key used by the application.

### 4. Start the frontend

In a second terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open the Vite URL, normally `http://localhost:5173`. If port 5173 is occupied, Vite may use `http://localhost:5174`; the backend's default CORS configuration allows both. The checked-in `.env.example` lists only port 5173, so add 5174 if you copy that file and use the fallback port.

The frontend uses `http://localhost:8080` as its default API base URL. To point it at another backend, create `frontend/.env.local`:

```dotenv
VITE_API_BASE_URL=https://exams.example.edu
```

Do not put database credentials, JWT secrets, or setup tokens in `VITE_*` variables. Vite embeds those values into the public frontend bundle.

## Configuration

Spring does not automatically load the root `.env` file. `.env.example` is a reference for the available variables; set values through your shell, IDE, container runtime, or platform secret manager.

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `SPRING_DATASOURCE_URL` | Yes | None | JDBC URL for MySQL, for example `jdbc:mysql://localhost:3306/campus_exam_scheduler?serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | Yes | None | MySQL application user |
| `SPRING_DATASOURCE_PASSWORD` | Yes | None | MySQL application password |
| `JWT_SECRET` | Yes | None | Signing secret for administrator access tokens and student verification tokens; use at least 32 random bytes |
| `ADMIN_SETUP_TOKEN` | No | Empty | Enables the one-time first-administrator bootstrap endpoint when non-blank |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173,http://localhost:5174` | Comma-separated exact frontend origins |
| `SPRING_JPA_SHOW_SQL` | No | `false` | Enables Hibernate SQL logging |
| `FLYWAY_ENABLED` | No | `true` | Enables Flyway on startup; tests disable it programmatically |
| `TESSERACT_CMD` | No | Auto-detected | Full path to the Tesseract executable for scanned timetable PDFs |
| `VITE_API_BASE_URL` | No | `http://localhost:8080` | Frontend build-time API base URL |
| `TEST_DATASOURCE_URL` | Tests | `SPRING_DATASOURCE_URL` | Test JDBC URL; must target exactly `campus_exam_scheduler` |
| `TEST_DATASOURCE_USERNAME` | Tests | `SPRING_DATASOURCE_USERNAME`, then `root` | Test database user |
| `TEST_DATASOURCE_PASSWORD` | Tests | `SPRING_DATASOURCE_PASSWORD`, then empty | Test database password |

`CORS_ALLOWED_ORIGINS` accepts exact origins only. Add every frontend origin that will call the API, including the `https` production origin and any local development port in use.

## Database and migrations

Flyway is the source of truth for schema changes:

- `V1__baseline_schema.sql` creates the baseline tables for a fresh installation.
- `V2__student_dob_and_absentee_audit.sql` adds `students.date_of_birth` and the `source`, `locked`, `updated_at`, and `updated_by` audit fields on absentee assignments.

The application uses `spring.jpa.hibernate.ddl-auto=validate`. Do not change it to `update` or `create`; Hibernate should validate Flyway's schema, not modify it.

For an existing database:

1. Take a verified backup before starting the application.
2. Confirm the current `flyway_schema_history` state if the database has been migrated before.
3. Start the backend and review the Flyway log. An existing schema without Flyway history is baselined at V1 and then receives V2.

If a legacy schema already contains the V2 columns but has no Flyway history, V2 will fail. Restore the backup and reconcile the baseline and migration history with a database administrator rather than deleting data or editing an applied migration.

Do not edit a migration that has already been applied. Flyway validates checksums. Add a new `V3__...sql` migration instead. The files in `database/migrations/` are legacy manual snippets retained for reference; the authoritative migration path is `backend/src/main/resources/db/migration/`.

`database/demo-data.sql` is optional, inserts synthetic records only, and is not executed automatically. Do not run it against production data.

## First administrator setup

1. Start the backend with `ADMIN_SETUP_TOKEN` set to a random value.
2. Open `http://localhost:5173/admin/setup`.
3. Enter the setup token, administrator name, email, and a password of 8 to 72 characters.
4. Sign in at `/admin/login`.

The bootstrap endpoint is rejected once any administrator exists. After setup, unset `ADMIN_SETUP_TOKEN` and restart the backend as defense in depth. Additional administrators can be created through the authenticated `/api/admins` endpoints; the current frontend does not include a dedicated administrator-management screen.

The signed-in administrator can change their own password from the dashboard. The change requires the current password and a new password of at least 8 characters.

## Import workflows

### Student roster import

Open **Student Management -> Import Students** and upload a CSV, XLS, or XLSX file. The first worksheet is read, and a title or notes may appear above the header row.

| Column | Required | Notes |
| --- | --- | --- |
| Name | Yes | 2 to 100 characters |
| Email | Yes | Valid address, maximum 100 characters |
| Password | Yes | 8 to 72 characters; hashed with BCrypt before saving |
| Roll Number or Enrollment Number | Yes | Enrollment numbers are normalized to uppercase |
| Semester | Yes | Integer from 1 to 8 |
| Department Name or Department ID | Yes | Must match an existing department |
| Date of Birth | No | `YYYY-MM-DD`; required later for student self-service verification |

Common header aliases are accepted, including `Roll No`, `Enrollment`, `Dept`, `DOB`, and `Registration Number`. The preview hides passwords, reports row-level errors, and shows the number of records that will be saved. Duplicate emails and enrollment numbers in the file or database are skipped. If any row has a validation error, the import is rejected without partial writes.

Populate dates of birth for existing students before enabling self-service. A student without a stored date of birth cannot complete enrollment verification, even if all other fields are present.

### Timetable import

Open **Exam Management -> Import timetable** and choose `Regular` or `Absentee` as the exam type. Upload an XLS, XLSX, or PDF file. The first Excel worksheet is read. A row-based timetable requires:

| Column | Required | Example |
| --- | --- | --- |
| Subject Code | Yes | `CS101` |
| Date | Yes | `2026-11-12` |
| Time | Yes | `09:00` or `09:00 - 11:00` |
| Building | No | `Main Building`; defaults to `Not assigned` |
| Room | No | `204`; defaults to `Not assigned` |
| End Time | No | `11:00`; may also be supplied in the Time range |

Text-based PDFs support aligned rows and date-by-course matrices. Image-only PDFs are rendered and read with Tesseract. The backend looks for Tesseract in `TESSERACT_CMD`, then on `PATH`, then at the standard Windows install path. OCR requires the Tesseract engine and appropriate language data, and it works best with clear, high-resolution scans.

The preview reports invalid rows, uncertain OCR fields, duplicate rows, and conflicts before saving. An import with validation errors or unresolved conflicts is rejected without partial writes. Reimporting the same timetable skips exams already identified by subject, date, start time, and exam type.

Conflict checks cover room double-booking and same-department/semester cohort clashes. Parallel section variants of the same paper family are allowed to share a slot. Absentee papers are compared separately from regular papers for student clashes.

### Absentee papers

Create or import exams with the `ABSENTEE` type, then open **Student Management -> Manage absentee papers** for a student. The administrator can assign absentee papers that belong to the student's department and semester, and can lock individual assignments so the student cannot remove them.

The administrator save is authoritative: the submitted list replaces the student's full assignment set, and the submitted lock list controls which rows are protected. The student self-service save preserves every locked row even when it is omitted from the request; only unlocked rows can be added or removed by the student.

After a student verifies their enrollment number and date of birth, the student can open the absentee-paper dialog from their schedule, select available papers, and save. An assigned absentee paper replaces the corresponding regular paper in the student's schedule. Only assigned absentee papers appear.

### Seating plans

Administrators can upload a text-based seating-plan PDF from **Seating Plans**. The parser recognizes room headings and roll numbers in formats such as `A1-16`, and records room, row, column, and section where available. Reimporting a plan with the same title, department, and semester replaces its seat assignments.

Students can open **Seating Lookup**, choose a published plan, and enter a roll number in the same format. Scanned or image-only seating-plan PDFs are not supported.

## Student schedules and calendar export

Students do not need an account to view a schedule:

- Search by enrollment number from the home page.
- Browse and filter exams at `/exams` by query, semester, date, and exam type.
- Save exams to **My Schedule**. The selection is stored in the current browser's local storage, not on the server.
- Export saved exams as `my-exam-schedule.ics` from `/calendar`.

Calendar events use an imported end time when available. For exams without an end time, the export uses the fallback duration selected in **My Schedule**. Event times are interpreted as India Standard Time.

Schedule search is intentionally public by enrollment number. The short-lived verification token is only required to read or change absentee-paper eligibility, not to view the published schedule.

## API overview

All endpoints return JSON unless noted. Multipart endpoints accept a `file` part and are limited to 10 MB.

| Area | Endpoints | Access |
| --- | --- | --- |
| Authentication | `POST /api/auth/login`, `POST /api/auth/register`, `PUT /api/auth/password` | Login and bootstrap are public; bootstrap also requires `X-Admin-Setup-Token`; password change requires an administrator JWT |
| Administrator CRUD | `/api/admins`, `/api/departments`, `/api/subjects`, `/api/students`, `/api/exams` | Administrator JWT |
| Public exam search | `GET /api/exams/search?query=&semester=&date=&examType=` | Public |
| Public schedule | `GET /api/students/{enrollmentNumber}/schedule` | Public |
| Student verification | `POST /api/students/{enrollmentNumber}/verify` with `{ "dateOfBirth": "YYYY-MM-DD" }` | Public and rate-limited; returns a 10-minute verification token |
| Student absentee eligibility | `GET` and `PUT /api/students/{enrollmentNumber}/absentee-eligibility` | Requires `X-Student-Verification` for the same enrollment number |
| Administrator absentee management | `GET` and `PUT /api/students/{studentId}/absentee-exams` | Administrator JWT |
| Student import | `POST /api/students/import/preview`, `POST /api/students/import` | Administrator JWT; multipart CSV/XLS/XLSX |
| Timetable import | `POST /api/exams/import/preview`, `POST /api/exams/import` | Administrator JWT; multipart XLS/XLSX/PDF; optional `examType` of `REGULAR` or `ABSENTEE` |
| Seating import | `POST /api/seating/plans/import` | Administrator JWT; multipart PDF; optional `title` |
| Public seating | `GET /api/seating/plans`, `GET /api/seating/plans/{planId}/roll/{rollNumber}` | Public |
| API documentation | `/swagger-ui/index.html`, `/v3/api-docs` | Public by default |

Every route not listed above requires a valid administrator JWT in the `Authorization: Bearer <token>` header.

Swagger UI is available while the backend is running at `http://localhost:8080/swagger-ui/index.html`. The current security configuration permits Swagger and OpenAPI routes publicly. In production, disable them with `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false`, or restrict them at the reverse proxy.

## Security model

| Control | Current behavior |
| --- | --- |
| Administrator authentication | Stateless JWT access token, valid for 24 hours |
| Password storage | BCrypt; new passwords are 8 to 72 characters |
| Student verification | Enrollment number plus date of birth; scoped verification token valid for 10 minutes |
| Login and verification throttling | 5 failures per 15-minute window per email or enrollment number; returns `429` with `Retry-After` |
| First-administrator bootstrap | Requires `X-Admin-Setup-Token`; constant-time token comparison; rejected once an administrator exists |
| CORS | Exact configured origins; default local origins are `http://localhost:5173` and `http://localhost:5174` |
| Uploads | 10 MB maximum request and file size |
| Schema changes | Flyway migrations; Hibernate `ddl-auto=validate` |

Operational expectations:

- Serve the frontend and API over HTTPS in production.
- Use a unique, random JWT secret and setup token. Store them in a secret manager, not in `.env` files committed to Git.
- Grant the application database user only the privileges it needs on `campus_exam_scheduler`.
- Back up the database before applying migrations or importing production data.
- Do not expose database credentials, JWTs, setup tokens, or student dates of birth in logs or frontend bundle variables.
- The student verification token is deliberately short-lived and scoped to one enrollment number. It is not an administrator token and cannot access administrator routes.

### Known limitations

- **Date-of-birth backfill:** Self-service absentee management only works for students with a stored `date_of_birth`. Existing records without one must be updated by an administrator or a controlled data backfill before those students can verify.
- **No immediate JWT revocation:** Changing an administrator password does not invalidate JWTs already issued to that account; they remain valid until their 24-hour expiry. Rotating `JWT_SECRET` invalidates all tokens and signs every administrator out.
- **Process-local rate limiting:** The failure limiter is held in memory. A multi-instance deployment needs a shared store such as Redis or rate limiting at the API gateway.
- **Replace-all administrator saves:** Administrator absentee saves replace the complete assignment set. Concurrent edits can overwrite each other, so refresh before saving when multiple administrators may be working on the same student.
- **Tests use the application schema:** The backend test guard requires the `campus_exam_scheduler` schema and disables Flyway. Tests are transactional and roll back their own records, but they are not isolated from existing data. Run them only against a local or disposable database.
- **Docker image does not include OCR:** The checked-in backend image does not install Tesseract. Text-based PDFs work, but scanned timetable PDF import requires a custom image with Tesseract and language data, or running the backend on a host where Tesseract is installed.
- **Public Swagger UI:** Swagger and OpenAPI routes are public by default. Restrict or disable them in production.
- **No student sign-in:** The student experience is a public schedule lookup plus date-of-birth verification for absentee self-service. There is no student password login or server-side student session.

## Testing

### Backend tests

The integration tests target the existing `campus_exam_scheduler` schema and deliberately reject a separate test database. They disable Flyway and Hibernate DDL, use transactions, and roll back the unique records they create. Do not point them at production.

Set explicit test credentials and run:

```powershell
$env:TEST_DATASOURCE_URL = "jdbc:mysql://localhost:3306/campus_exam_scheduler?serverTimezone=UTC"
$env:TEST_DATASOURCE_USERNAME = "campus_app"
$env:TEST_DATASOURCE_PASSWORD = "replace-with-your-local-test-password"

Set-Location backend
.\mvnw.cmd test
```

The test URL must target exactly the `campus_exam_scheduler` database, must not embed credentials, and must not use `createDatabaseIfNotExist`. The suite covers authentication and rate limiting, student verification, absentee lock semantics, timetable import, conflict detection, and test-database guardrails. There is no checked-in CI workflow; wire these commands into your pipeline.

### Frontend checks

```bash
cd frontend
npm run lint
npm run build
```

The production build is written to `frontend/dist/`. To preview it locally, run `npm run preview`.

## Build and deployment

### Backend JAR

```powershell
Set-Location backend
.\mvnw.cmd clean package -DskipTests
java -jar target/campus-exam-scheduler-0.0.1-SNAPSHOT.jar
```

Run the tests separately before packaging for a release. Supply all required environment variables to the process that starts the JAR.

### Backend Docker image

The Dockerfile expects the repository root as its build context because it copies `backend/` into the image:

```powershell
docker build -t campus-exam-scheduler-backend -f backend/Dockerfile .
docker run --rm -p 8080:8080 `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/campus_exam_scheduler?serverTimezone=UTC" `
  -e SPRING_DATASOURCE_USERNAME="campus_app" `
  -e SPRING_DATASOURCE_PASSWORD="replace-with-your-db-password" `
  -e JWT_SECRET="replace-with-a-random-secret-at-least-32-bytes" `
  -e CORS_ALLOWED_ORIGINS="https://exams.example.edu" `
  campus-exam-scheduler-backend
```

On Docker Desktop, `host.docker.internal` reaches MySQL on the host. On Linux, use the database host's reachable address or a container network. The image does not include Tesseract, so scanned timetable OCR requires a custom image. The repository does not include a Compose file; run MySQL separately or provide your own orchestration.

### Frontend deployment

Build the frontend with `npm ci && npm run build`, then serve `frontend/dist/` from a static host or reverse proxy. Because the application uses client-side routing, configure the host to fall back to `index.html` for unknown paths. Set `VITE_API_BASE_URL` at build time to the public backend URL, and add that frontend origin to `CORS_ALLOWED_ORIGINS`.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Backend fails at startup with a missing `JWT_SECRET` value | Set `JWT_SECRET` in the backend process environment |
| `Communications link failure` or access denied | Confirm MySQL is running, the database exists, and the URL and credentials are correct |
| `Schema-validation` failure or missing column | Confirm Flyway ran successfully and the database matches the migrations; do not switch to `ddl-auto=update` |
| Flyway checksum or validation failure | Do not edit applied migrations; restore from backup or add a new migration after reviewing `flyway_schema_history` |
| Administrator receives `401` | The token may be expired, malformed, or signed with a different `JWT_SECRET`; sign in again |
| Login or verification returns `429` | Wait for the `Retry-After` period; the limiter allows 5 failures per 15 minutes |
| Browser blocks an API request with a CORS error | Add the exact frontend origin, including scheme and port, to `CORS_ALLOWED_ORIGINS` |
| Vite opened on port 5174 and API calls fail | Add `http://localhost:5174` to `CORS_ALLOWED_ORIGINS` |
| Student verification always fails | Confirm the student has a `date_of_birth` set and the submitted date matches exactly |
| Scanned PDF import says OCR is unavailable | Install Tesseract, set `TESSERACT_CMD`, or use a text-based PDF or Excel file |
| Backend tests reject the test database | Use a URL ending in `/campus_exam_scheduler`; separate test schemas are intentionally rejected |

## Contributing

Before opening a pull request:

1. Run the backend tests with explicit test credentials.
2. Run `npm run lint` and `npm run build` in `frontend/`.
3. Keep schema changes additive and add a new Flyway migration rather than editing an applied one.
4. Do not commit `.env` files, logs, generated build output, setup tokens, JWT secrets, or real student data.

## License

No open-source license is currently included in this repository. The project is maintained for academic and institutional exam-scheduling use. Contact the maintainer before redistributing or deploying it outside that context.
