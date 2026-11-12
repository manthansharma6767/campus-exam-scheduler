package com.manthan.campusexamscheduler.exception;

import com.manthan.campusexamscheduler.dto.StudentDobBackfillPreviewResponse;

public class StudentDobBackfillValidationException extends RuntimeException {

    private final StudentDobBackfillPreviewResponse preview;

    public StudentDobBackfillValidationException(StudentDobBackfillPreviewResponse preview) {
        super(preview.message());
        this.preview = preview;
    }

    public StudentDobBackfillPreviewResponse getPreview() {
        return preview;
    }
}
