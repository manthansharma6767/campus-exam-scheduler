package com.manthan.campusexamscheduler.dto;

import java.util.List;

public record ImportPreviewResponse(
        int validRows,
        int skippedDuplicates,
        boolean canImport,
        boolean previewTruncated,
        List<ImportPreviewRow> rows,
        List<ImportRowError> errors,
        List<ConflictDetail> conflicts,
        String message) { }
