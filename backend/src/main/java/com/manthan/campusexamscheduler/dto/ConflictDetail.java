package com.manthan.campusexamscheduler.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConflictDetail {
    int row;
    String type;
    String message;
}
