package com.manthan.campusexamscheduler.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SeatingPlanImportResponse {
    SeatingPlanResponse plan;
    int importedSeats;
    int duplicateSeatsIgnored;
    boolean replacedExistingPlan;
    String message;
}
