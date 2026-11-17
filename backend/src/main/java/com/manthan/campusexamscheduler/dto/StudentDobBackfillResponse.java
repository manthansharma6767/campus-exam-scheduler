package com.manthan.campusexamscheduler.dto;

public record StudentDobBackfillResponse(
        int updated,
        int overwritten,
        int alreadyPopulated,
        int notFound,
        int totalRows,
        String message) { }
