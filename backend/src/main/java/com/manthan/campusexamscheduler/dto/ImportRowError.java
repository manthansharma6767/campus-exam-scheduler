package com.manthan.campusexamscheduler.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImportRowError {
    int row;
    String message;
}
