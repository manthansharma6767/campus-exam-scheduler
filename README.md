# Campus Exam Scheduler

Campus Exam Scheduler is a full-stack application for publishing examination timetables, managing academic records, importing roster and timetable files, and helping students find and save their personal exam schedules.

## Features

### Administration

- One-time, token-protected first administrator setup
- JWT-protected administration area
- Department, subject, student, and exam CRUD management
- Student roster import from CSV, XLS, and XLSX
- Timetable import from XLS, XLSX, and text-based PDF files
- Import previews with row-level validation errors
- Duplicate detection and safe, repeatable imports
- Timetable conflict detection for overlapping rooms and student cohorts
- Seating-plan PDF import and roll-number seat lookup

### Student experience

- Search a personal schedule by enrollment number
- Browse and filter published exams by subject, course code, venue, semester, and date
- Add or remove exams from **My Schedule**
- Export saved exams as an iCalendar (`.ics`) file

## Technology

| Layer | Technologies |
| --- | --- |
| Backend | Java 17, Spring Boot, Spring Security, Spring Data JPA, JWT |
| Database | MySQL 8+ |
| File processing | Apache POI, Apache PDFBox |
| Frontend | React 19, Vite, Material UI, React Router, Axios |
| API documentation | Springdoc OpenAPI / Swagger UI |

## Repository layout

```text
.
├── backend/       # Spring Boot application, tests, Maven wrapper, and Dockerfile
├── database/      # Optional SQL scripts and seed data
├── frontend/      # React/Vite application
├── postman/       # API workspace resources
└── README.md
```

## Prerequisites

- Java 17 or newer
- Node.js and npm
- MySQL 8 or newer
- Git

## Local setup

### 1. Clone the repository

```bash
git clone https://github.com/manthansharma6767/campus-exam-scheduler.git
cd campus-exam-scheduler
```

### 2. Create a MySQL database

Create a database dedicated to this local installation:

```sql
CREATE DATABASE campus_exam_scheduler;
```

Do not use production credentials or a database containing real data for local testing.

### 3. Configure and run the backend

From the repository root, open a PowerShell terminal and set the required environment variables. Keep credentials and secrets in your IDE run configuration or local environment—not in source files.

```powershell
Set-Location backend
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/campus_exam_scheduler?serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME = "<your-mysql-username>"
$env:SPRING_DATASOURCE_PASSWORD = "<your-mysql-password>"
$env:JWT_SECRET = "<random-secret-at-least-32-characters>"
$env:ADMIN_SETUP_TOKEN = "<random-one-time-setup-token>"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

The API starts at `http://localhost:8080`. The backend requires `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET`; there are no database or JWT secret fallbacks in source configuration.

### 4. Run the frontend

In a second terminal:

```powershell
Set-Location frontend
npm install
npm run dev
```

Open the Vite URL, normally `http://localhost:5173`. The frontend uses `http://localhost:8080` as its default API URL. To use another backend, create `frontend/.env.local`:

```dotenv
VITE_API_BASE_URL=https://your-backend.example.com
```

Never put database credentials, JWT secrets, or administrator setup tokens in `VITE_*` variables because they are bundled into the public frontend.

## First administrator setup

1. Start the backend with `ADMIN_SETUP_TOKEN` configured.
2. Open `http://localhost:5173/admin/setup`.
3. Enter the setup token and choose your own administrator name, email, and password.
4. Sign in at `/admin/login`.

The setup endpoint is disabled after the first administrator has been created.

## Import workflows

### Student roster import

Open **Student Management → Import Students** and upload a CSV, XLS, or XLSX file. Required columns are:

| Name | Email | Password | Roll Number or Enrollment Number | Semester | Department |
| --- | --- | --- | --- | --- | --- |

Departments can be identified by name or ID. The import preview hides passwords and reports row-level errors, duplicates, and the number of records that will be saved. Invalid imports are rejected atomically; duplicate emails or enrollment numbers are skipped.

### Timetable import

Open **Exam Management → Import timetable** and upload an XLS, XLSX, or text-based PDF file. A row-based timetable can use:

| Subject Code | Date | Time | Building | Room |
| --- | --- | --- | --- | --- |
| CS101 | 2026-11-12 | 09:00 | Main Building | 204 |

Excel imports read the first worksheet and allow a title above the header. PDFs must contain selectable text; scanned PDFs require OCR and are not supported. The preview reports invalid rows, duplicates, and room or cohort conflicts before saving. Imports with validation errors or conflicts are rejected without partial writes. Reimporting the same timetable skips existing exams identified by subject, date, and start time.

### Seating plans

Administrators can upload a supported seating-plan PDF from **Seating Plans**. Students can search the published plans by roll number from the public seating lookup page.

## Student schedules and calendar export

Students can search by enrollment number or browse published exams without logging in. Selected exams are stored in the current browser and are available from **My Schedule**. The export action downloads `my-exam-schedule.ics`, using India Standard Time for event times.

## Development commands

Run backend tests:

```powershell
Set-Location backend
.\mvnw.cmd test
```

Run frontend checks:

```powershell
Set-Location frontend
npm run lint
npm run build
```

When the backend is running, Swagger UI is available at:

`http://localhost:8080/swagger-ui/index.html`

Build the backend Docker image from the repository root:

```powershell
docker build -f backend/Dockerfile backend
```

## Security and data handling

- Never commit passwords, tokens, database URLs containing credentials, or `.env` files.
- Use a disposable MySQL schema for development and testing.
- The application uses `ddl-auto=update`; review database permissions and backups before connecting it to an existing schema.
- Do not use the demo SQL script against production data.

## License

This project is maintained for academic and institutional exam-scheduling use.
