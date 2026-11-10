-- Track which absentee papers each student is eligible to appear for.
-- Regular exams remain cohort-wide; only ABSENTEE exams require a mapping.
-- Additive and safe to run more than once.
CREATE TABLE IF NOT EXISTS student_absentee_exams (
    student_absentee_exam_id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_absentee_exam_id),
    UNIQUE KEY uk_student_absentee_exam (student_id, exam_id),
    KEY idx_student_absentee_exam_exam (exam_id),
    CONSTRAINT fk_student_absentee_exam_student
        FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE,
    CONSTRAINT fk_student_absentee_exam_exam
        FOREIGN KEY (exam_id) REFERENCES exams (exam_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Hibernate may have created the table first on a running development server.
-- Ensure the timestamp default also exists on that already-created table.
ALTER TABLE student_absentee_exams
    ALTER COLUMN assigned_at SET DEFAULT (CURRENT_TIMESTAMP);

-- Live data: A2-22 was absent for Cloud Computing only.
-- All other regular papers remain visible through the cohort schedule.
INSERT INTO student_absentee_exams (student_id, exam_id, assigned_at)
SELECT st.student_id, e.exam_id, CURRENT_TIMESTAMP
FROM students st
JOIN exams e ON e.exam_type = 'ABSENTEE'
JOIN subjects sub ON sub.subject_id = e.subject_id
WHERE UPPER(st.enrollment_number) = UPPER('A2-22')
  AND sub.subject_code = '24CS01TH0503'
  AND NOT EXISTS (
      SELECT 1
      FROM student_absentee_exams sae
      WHERE sae.student_id = st.student_id
        AND sae.exam_id = e.exam_id
  );
