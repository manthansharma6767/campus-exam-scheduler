# Campus Exam Scheduler

A full-stack application for managing college examination schedules. Administrators can manage academic records and import timetables, while students can look up exams by enrollment number and export a personal schedule to their calendar.

## Demo

- **Web application:** [campus-exam-scheduler-frontend.vercel.app](https://campus-exam-scheduler-frontend.vercel.app/)
- **Backend service:** [campus-exam-scheduler.onrender.com](https://campus-exam-scheduler.onrender.com)

## Features

### Administration

- Create the first administrator through a token-protected setup flow.
- Manage departments, subjects, students, and exams.
- Import student rosters from CSV or Excel, including roll/enrollment numbers.
- Import exam timetables from Excel (`.xls`, `.xlsx`) and text-based PDF files.
- Validate imported rows against existing subject codes and skip duplicate exams.
- Protect administrative routes with Spring Security and JWT authentication.

### Student scheduling

- Search for exams by enrollment number.
- Browse published exams by course, venue, semester, or date without an enrollment number.
- Add individual exams or the full result set to **My Schedule**.
- Export a schedule as an iCalendar (`.ics`) file for calendar applications.

## Technology stack

| Area | Technologies |
| --- | --- |
| Backend | Java 17, Spring Boot, Spring Security, Spring Data JPA, JWT |
| Database | MySQL 8+ |
| File import | Apache POI, Apache PDFBox |
| Frontend | React 19, Vite, Material UI, React Router, Axios |
| API documentation | Springdoc OpenAPI / Swagger UI |

## Repository structure

```text
.
<<<<<<< HEAD
├── backend/             # Spring Boot application, tests, Maven wrapper, and Dockerfile
├── frontend/            # React/Vite web application
├── database/             # Database scripts and seed data
=======
├── backend/             # Spring Boot application, tests, and Maven wrapper
├── database/            # SQL seed scripts
├── frontend/            # React/Vite web application
>>>>>>> 156a3a3 (Update README documentation)
├── postman/             # Postman workspace configuration
└── README.md
```

## Getting started

### Prerequisites

- Java 17 or later
- Node.js and npm
- MySQL 8 or later

### 1. Clone the repository and create a database

```bash
git clone https://github.com/manthansharma6767/campus-exam-scheduler.git
cd campus-exam-scheduler
```

Create a MySQL database for the application:

```sql
CREATE DATABASE campus_exam_scheduler;
```

### 2. Configure and start the backend

From the repository root, use these commands to configure and start the backend from `backend/`. In an IDE, set the same variables in the backend run configuration. The example below is for PowerShell; replace the local database username and password with your own values. The backend has no embedded database credentials or JWT signing-key fallback.

```powershell
cd backend
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/campus_exam_scheduler?serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "<your-local-mysql-password>"
$env:JWT_SECRET = "<a-random-secret-at-least-32-characters-long>"
$env:ADMIN_SETUP_TOKEN = "<a-long-random-one-time-setup-token>"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

The backend runs at `http://localhost:8080` by default. Keep the database credentials, JWT secret, and administrator setup token outside source control.

### 3. Start the frontend

In a second terminal, from `frontend/`:

```bash
cd frontend
npm install
npm run dev
```

Open the local URL printed by Vite (usually `http://localhost:5173`). The frontend uses `http://localhost:8080` as its default API address. To use another backend, create `frontend/.env.local`:

```dotenv
VITE_API_BASE_URL=https://your-backend.example.com
```

Values with the `VITE_` prefix are included in the public frontend build. Do not put database credentials, JWT secrets, or other private values in them.

## Initial administrator setup

With both applications running, open `http://localhost:5173/admin/setup` and create the first administrator using the configured `ADMIN_SETUP_TOKEN`. The setup endpoint is disabled after an administrator has been created. Then sign in at `/admin/login`.

## Importing a timetable

In the admin application, open **Exam Management → Import timetable**. Imports support `.xls`, `.xlsx`, and text-based `.pdf` files up to 10 MB. Scanned PDFs are not supported because they require OCR.

For row-based files, use one exam per row with these columns:

| Subject Code | Date | Time | Building | Room |
| --- | --- | --- | --- | --- |
| CS101 | 2026-11-12 | 09:00 | Main Building | 204 |

Subject codes must already exist in Subject Management. Excel imports read the first worksheet; a title above the header is allowed. PDFs must contain selectable text and can use aligned exam rows or date-by-course matrices. Missing venues are saved as **Not assigned**.

The admin can preview parsed rows before confirming an import. Timetable rows may provide a single `Time` value, a time range such as `09:00–12:00`, or separate `Start Time` and `End Time` columns. The preview reports row-level validation errors, duplicate rows, and overlapping room or student-cohort conflicts. An import with validation errors or conflicts is rejected without saving any rows. Existing exams with the same subject, date, and start time are skipped to prevent duplicates when a timetable is uploaded again. End times are optional for older records and for timetables that only provide a start time; when a duration is unknown, the scheduler checks only matching start times rather than guessing an exam length.

## Importing students

In **Student Management**, choose **Import Students** and upload a CSV, XLS, or XLSX file. Excel imports read the first worksheet. The header row must include `Name`, `Email`, `Password`, `Roll Number` (or `Enrollment Number`), `Semester`, and `Department` (department name or ID). A CSV template is available in the import dialog. Department names must match an existing department. Passwords are hashed before they are stored.

The admin can preview parsed student records before confirming an import. Passwords are never shown in the preview. Rows with an existing or repeated email/roll number are skipped; if any other row is invalid, the import is rejected as a whole and no students are saved. Supported semester values are 1 through 8.

## Student schedules and calendar export

Students can search by enrollment number or browse published exams by course, venue, semester, or date. Selected exams are stored in the current browser and can be opened from **My Schedule**; selections do not sync between browsers or devices. The `.ics` export can be imported into Google Calendar, Outlook, Apple Calendar, and other calendar applications. Exported exam times use India Standard Time.

## Development commands

Run backend tests from `backend/`:

```powershell
Set-Location backend
.\mvnw.cmd test
```

Run frontend checks from `frontend/`:

```bash
npm run lint
npm run build
```

When the backend is running, the API reference is available at `http://localhost:8080/swagger-ui/index.html`.

The backend Dockerfile is in `backend/`; from the repository root, build it with `docker build -f backend/Dockerfile .`.
