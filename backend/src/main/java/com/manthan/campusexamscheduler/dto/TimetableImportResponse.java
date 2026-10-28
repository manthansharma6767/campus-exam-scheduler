package com.manthan.campusexamscheduler.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TimetableImportResponse {
    int imported;
    int skippedDuplicates;
    int totalRows;
    String message;
    List<ImportRowError> rowErrors;
    List<ConflictDetail> conflicts;
}
