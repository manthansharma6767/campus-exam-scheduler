-- Add an explicit exam type so regular and absentee papers stay distinguishable.
-- Existing rows are preserved and default to REGULAR. Safe to run more than once.
SET @exam_type_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'exams'
      AND column_name = 'exam_type'
);

SET @add_exam_type := IF(
    @exam_type_exists = 0,
    'ALTER TABLE exams ADD COLUMN exam_type VARCHAR(20) NOT NULL DEFAULT ''REGULAR'' AFTER subject_id',
    'SELECT ''exams.exam_type already exists'' AS status'
);

PREPARE add_exam_type_statement FROM @add_exam_type;
EXECUTE add_exam_type_statement;
DEALLOCATE PREPARE add_exam_type_statement;
