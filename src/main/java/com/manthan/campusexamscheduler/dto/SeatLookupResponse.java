package com.manthan.campusexamscheduler.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SeatLookupResponse {
    Long seatingPlanId;
    String planTitle;
    String departmentName;
    Integer semester;
    String rollNumber;
    String section;
    String roomNumber;
    Integer rowNumber;
    Integer columnNumber;
    Integer sourcePage;
}
