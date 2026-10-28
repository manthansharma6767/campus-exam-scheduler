-- Optional, idempotent demo data for campus_exam_scheduler.
-- This script inserts only synthetic records and is not run automatically.

START TRANSACTION;

INSERT INTO departments (department_name)
SELECT 'Demo Computing'
WHERE NOT EXISTS (
    SELECT 1 FROM departments WHERE department_name = 'Demo Computing'
);

INSERT INTO subjects (subject_code, subject_name, semester, department_id)
SELECT 'DEMO501', 'Demo Exam Scheduling', 5, d.department_id
FROM departments d
WHERE d.department_name = 'Demo Computing'
  AND NOT EXISTS (
      SELECT 1 FROM subjects s WHERE s.subject_code = 'DEMO501'
  );

INSERT INTO subjects (subject_code, subject_name, semester, department_id)
SELECT 'DEMO502', 'Demo Timetable Operations', 5, d.department_id
FROM departments d
WHERE d.department_name = 'Demo Computing'
  AND NOT EXISTS (
      SELECT 1 FROM subjects s WHERE s.subject_code = 'DEMO502'
  );

INSERT INTO students (name, email, password, enrollment_number, semester, department_id)
SELECT 'Demo Student', 'demo.student@example.test',
       '$2a$10$HuM37bjEQdqdhIDdA3WzxezU6unwzwjurHhgp/0DMdTmO.OdxYBOW',
       'DEMO-2026-001', 5, d.department_id
FROM departments d
WHERE d.department_name = 'Demo Computing'
  AND NOT EXISTS (
      SELECT 1 FROM students s
      WHERE s.email = 'demo.student@example.test'
         OR s.enrollment_number = 'DEMO-2026-001'
  );

INSERT INTO exams (exam_date, exam_time, exam_end_time, room_number, building, subject_id)
SELECT '2026-11-18', '09:00:00', '11:00:00', 'D-501', 'Demo Building', s.subject_id
FROM subjects s
WHERE s.subject_code = 'DEMO501'
  AND NOT EXISTS (
      SELECT 1 FROM exams e
      WHERE e.subject_id = s.subject_id
        AND e.exam_date = '2026-11-18'
        AND e.exam_time = '09:00:00'
  );

INSERT INTO exams (exam_date, exam_time, exam_end_time, room_number, building, subject_id)
SELECT '2026-11-20', '13:00:00', '15:00:00', 'D-502', 'Demo Building', s.subject_id
FROM subjects s
WHERE s.subject_code = 'DEMO502'
  AND NOT EXISTS (
      SELECT 1 FROM exams e
      WHERE e.subject_id = s.subject_id
        AND e.exam_date = '2026-11-20'
        AND e.exam_time = '13:00:00'
  );

COMMIT;
