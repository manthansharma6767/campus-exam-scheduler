package com.manthan.campusexamscheduler.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAbsenteeEligibilityRequest {

    @NotNull
    private List<Long> examIds;
}
