package com.manthan.campusexamscheduler.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SeatingPlanResponse {
    Long seatingPlanId;
    String title;
    String departmentName;
    Integer semester;
    Instant importedAt;
    long seatCount;
    long roomCount;
}
