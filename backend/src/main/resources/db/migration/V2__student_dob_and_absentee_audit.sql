-- Student self-service verification and protected admin absentee assignments.

ALTER TABLE students
    ADD COLUMN date_of_birth DATE NULL AFTER semester;

ALTER TABLE student_absentee_exams
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'STUDENT' AFTER assigned_at,
    ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0 AFTER source,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER locked,
    ADD COLUMN updated_by VARCHAR(255) NULL AFTER updated_at;

-- Existing mappings were created by administrators before source tracking
-- existed. Mark them as ADMIN but keep them unlocked so current students can
-- still manage their own selections until an administrator locks a row.
UPDATE student_absentee_exams
SET source = 'ADMIN'
WHERE source = 'STUDENT';
