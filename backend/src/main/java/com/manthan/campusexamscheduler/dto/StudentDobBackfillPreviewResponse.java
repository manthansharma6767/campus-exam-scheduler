package com.manthan.campusexamscheduler.dto;

import java.util.List;

public record StudentDobBackfillPreviewResponse(
        int eligibleRows,
        int alreadyPopulated,
        int notFound,
        boolean canApply,
        boolean previewTruncated,
        List<ImportPreviewRow> rows,
        List<ImportRowError> errors,
        String message) { }
