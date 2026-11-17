-- Record who changed a student's date of birth and when. The administrator
-- backfill writes these fields on every insert or overwrite.

ALTER TABLE students
    ADD COLUMN dob_updated_at DATETIME(6) NULL AFTER date_of_birth,
    ADD COLUMN dob_updated_by VARCHAR(255) NULL AFTER dob_updated_at;
