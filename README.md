# Campus Exam Scheduler

A full-stack web application designed to manage college examination schedules. The system allows administrators to manage departments, students, subjects, and examinations, while students can access their examination schedule using their enrollment number.

---

## Live Application

- **Frontend:** https://campus-exam-scheduler-frontend.vercel.app/
- **Backend API:** https://campus-exam-scheduler.onrender.com

---

## Project Overview

Campus Exam Scheduler provides a centralized platform for managing examination-related information in a college environment.

The application provides separate functionality for managing:

- Departments
- Students
- Subjects
- Examinations
- Student examination schedules
- Authentication and authorization

The React frontend communicates with the Spring Boot backend through REST APIs.

---

## Key Features

### Department Management

- Create departments
- View all departments
- View department by ID
- Update departments
- Delete departments
- Unique department names

### Student Management

- Create students
- View all students
- View student by ID
- Update students
- Delete students
- Unique email addresses
- Unique enrollment numbers
- Department association
- Semester management

### Subject Management

- Create subjects
- View all subjects
- View subject by ID
- Update subjects
- Delete subjects
- Unique subject codes
- Department association
- Semester-based subjects

### Examination Management

- Create examinations
- View all examinations
- View examination by ID
- Update examinations
- Delete examinations
- Associate examinations with subjects
- Store examination date
- Store examination time
- Store building information
- Store room number

### Student Examination Schedule

Students can retrieve their examination schedule using their enrollment number.

The schedule contains:

- Subject code
- Subject name
- Examination date
- Examination time
- Building
- Room number

---

## Authentication & Security

The application uses **Spring Security** with **JWT-based authentication**.

### Authentication Flow

```text
Login Request
      │
      ▼
AuthenticationManager
      │
      ▼
UserDetailsService
      │
      ▼
Password Verification
      │
      ▼
JWT Token Generated
      │
      ▼
Client
      │
      │ Authorization: Bearer <token>
      ▼
JwtAuthenticationFilter
      │
      ▼
JWT Validation
      │
      ▼
SecurityContext
      │
      ▼
Protected API

## Run locally

Requirements: Java 17+, Maven (or the included Maven wrapper), Node.js, npm, and MySQL 8+.

1. Use a MySQL 8 database (create `campus_exam_scheduler` locally, or use your hosted database).
2. In the backend environment, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. The URL must be a JDBC URL (`jdbc:mysql://...`), not a `mysql://` service URI. For an SSL-only hosted MySQL service, use `jdbc:mysql://<host>:<port>/<database>?sslMode=REQUIRED&serverTimezone=UTC`; set the username and rotated password in their separate environment variables. A saved MySQL Workbench connection does not configure the Spring backend.
3. Start the backend from PowerShell with `./mvnw.cmd spring-boot:run` (set `JAVA_HOME` to a Java 17 installation if needed).
4. In the frontend directory, run `npm install`, then `npm run dev`. Set `VITE_API_BASE_URL` to the backend URL if it is not `http://localhost:8080`.
5. Set `CORS_ALLOWED_ORIGINS` to the frontend origin (comma-separated if there are several). Before creating the first administrator, set a random `ADMIN_SETUP_TOKEN` in the backend environment and open `/admin/setup` in the frontend.

The application creates and updates its tables with Hibernate. Keep the database credentials, `ADMIN_SETUP_TOKEN`, and a strong `JWT_SECRET` in environment variables; do not put live credentials in this repository.

## Admin workflow

Create a department, add its subjects with their subject codes and semesters, then add students. Use passwords between 8 and 72 characters. A student's schedule is determined by the student's department and semester. Exam imports match the subject code to a subject already in the database.

## Importing an exam timetable

Sign in as an administrator and open **Exam Management → Import timetable**. The importer accepts `.xlsx`, `.xls`, and text-based `.pdf` files up to 10 MB. Scanned PDFs are not supported because they require OCR.

Use one exam per row with these columns (header matching ignores case and spaces):

| Subject Code | Date | Time | Building | Room |
| --- | --- | --- | --- | --- |
| CS101 | 2026-11-12 | 09:00 | Main Building | 204 |

The subject code must already exist in Subject Management. Dates accept common numeric and English month formats; times accept 24-hour or AM/PM formats. Excel uses the first worksheet and may have a title above the header. PDFs need selectable text and may use aligned exam rows or date-by-course matrices; scanned PDFs need OCR. Missing venues are saved as “Not assigned”.

The upload is processed as one transaction. If a row has invalid data or refers to an unknown subject, the import is rejected with the row number and no rows are saved. Existing exams with the same subject, date, and time are skipped, so re-uploading the same timetable does not create duplicates.

## Student schedule and calendar export

Students can search by enrollment number on the home page. On the result page, use **Add to my schedule** on individual exams or **Add all**. Saved selections stay in that browser and are available from **My Schedule** in the navigation; they do not sync across browsers or devices. The `.ics` export can be imported into Google Calendar, Outlook, Apple Calendar, and other calendar apps. Set the expected exam duration before exporting; imported events use India Standard Time.

## First administrator setup

Before exposing the backend, set a long random `ADMIN_SETUP_TOKEN` environment variable. Open `/admin/setup` and use that token to create the first administrator. The public setup endpoint refuses requests without the configured token and is disabled once an administrator exists. Configure a strong `JWT_SECRET` environment variable for production as well.
