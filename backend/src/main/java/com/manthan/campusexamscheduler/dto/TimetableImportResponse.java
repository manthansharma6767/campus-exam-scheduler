package com.manthan.campusexamscheduler.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TimetableImportResponse {
    int imported;
    int skippedDuplicates;
    String message;
}
