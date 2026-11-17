package com.manthan.campusexamscheduler.dto;

import java.util.List;

public record StudentDobBackfillPreviewResponse(
        int totalRows,
        int eligibleRows,
        int missingDobRows,
        int existingDobRows,
        int notFound,
        boolean overwriteExisting,
        boolean canApply,
        boolean previewTruncated,
        List<ImportPreviewRow> rows,
        List<ImportRowError> errors,
        String message) { }
