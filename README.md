# Campus Exam Scheduler

A full-stack web application designed to manage college examination schedules. The system allows administrators to manage departments, students, subjects, and examinations, while students can access their examination schedule using their enrollment number.

## Live Application

- **Frontend:** https://campus-exam-scheduler-frontend.vercel.app/
- **Backend API:** https://campus-exam-scheduler.onrender.com

## Project Overview

Campus Exam Scheduler provides a centralized platform for managing examination-related information in a college environment.

The application provides separate functionality for managing:

- Departments
- Students
- Subjects
- Examinations
- Student examination schedules
- Authentication and authorization

The frontend communicates with the Spring Boot backend through REST APIs.

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

## Authentication & Security

The application uses Spring Security with JWT-based authentication.

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
