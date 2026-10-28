package com.manthan.campusexamscheduler.exception;

import com.manthan.campusexamscheduler.dto.ImportPreviewResponse;

public class ImportValidationException extends RuntimeException {

    private final ImportPreviewResponse preview;

    public ImportValidationException(ImportPreviewResponse preview) {
        super(preview.message());
        this.preview = preview;
    }

    public ImportPreviewResponse getPreview() {
        return preview;
    }
}
