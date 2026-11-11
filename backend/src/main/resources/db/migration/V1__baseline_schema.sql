-- Baseline schema for fresh installations. Existing databases are baselined
-- at V1 by Flyway and continue with V2 and later migrations.

CREATE TABLE departments (
    department_id BIGINT NOT NULL AUTO_INCREMENT,
    department_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (department_id),
    UNIQUE KEY uk_departments_name (department_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE administrators (
    admin_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (admin_id),
    UNIQUE KEY uk_administrators_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE subjects (
    subject_id BIGINT NOT NULL AUTO_INCREMENT,
    subject_code VARCHAR(20) NOT NULL,
    subject_name VARCHAR(100) NOT NULL,
    semester INT NOT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (subject_id),
    UNIQUE KEY uk_subjects_code (subject_code),
    KEY idx_subjects_department (department_id),
    CONSTRAINT fk_subjects_department
        FOREIGN KEY (department_id) REFERENCES departments (department_id),
    CONSTRAINT chk_subjects_semester CHECK (semester BETWEEN 1 AND 8)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE students (
    student_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    enrollment_number VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    semester INT NOT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (student_id),
    UNIQUE KEY uk_students_email (email),
    UNIQUE KEY uk_students_enrollment (enrollment_number),
    KEY idx_students_department (department_id),
    CONSTRAINT fk_students_department
        FOREIGN KEY (department_id) REFERENCES departments (department_id),
    CONSTRAINT chk_students_semester CHECK (semester BETWEEN 1 AND 8)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE exams (
    exam_id BIGINT NOT NULL AUTO_INCREMENT,
    building VARCHAR(100) NOT NULL,
    exam_date DATE NOT NULL,
    exam_end_time TIME NULL,
    exam_time TIME NOT NULL,
    room_number VARCHAR(20) NOT NULL,
    subject_id BIGINT NOT NULL,
    exam_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    PRIMARY KEY (exam_id),
    KEY idx_exams_subject (subject_id),
    CONSTRAINT fk_exams_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (subject_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE seating_plans (
    seating_plan_id BIGINT NOT NULL AUTO_INCREMENT,
    department_name VARCHAR(150) NOT NULL,
    imported_at DATETIME(6) NOT NULL,
    semester INT NOT NULL,
    plan_title VARCHAR(200) NOT NULL,
    PRIMARY KEY (seating_plan_id),
    UNIQUE KEY uk_seating_plan_identity (plan_title, department_name, semester)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE seat_assignments (
    seat_assignment_id BIGINT NOT NULL AUTO_INCREMENT,
    column_number INT NOT NULL,
    roll_number VARCHAR(20) NOT NULL,
    room_number VARCHAR(30) NOT NULL,
    row_number INT NOT NULL,
    section_name VARCHAR(10) NULL,
    source_page INT NOT NULL,
    seating_plan_id BIGINT NOT NULL,
    PRIMARY KEY (seat_assignment_id),
    UNIQUE KEY uk_plan_roll_number (seating_plan_id, roll_number),
    CONSTRAINT fk_seat_assignments_plan
        FOREIGN KEY (seating_plan_id) REFERENCES seating_plans (seating_plan_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE student_absentee_exams (
    student_absentee_exam_id BIGINT NOT NULL AUTO_INCREMENT,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    PRIMARY KEY (student_absentee_exam_id),
    UNIQUE KEY uk_student_absentee_exam (student_id, exam_id),
    KEY idx_student_absentee_exam_exam (exam_id),
    CONSTRAINT fk_student_absentee_exam_exam
        FOREIGN KEY (exam_id) REFERENCES exams (exam_id),
    CONSTRAINT fk_student_absentee_exam_student
        FOREIGN KEY (student_id) REFERENCES students (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
