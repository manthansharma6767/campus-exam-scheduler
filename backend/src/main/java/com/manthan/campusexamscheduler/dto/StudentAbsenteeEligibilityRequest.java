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

    /**
     * Admin-only. Exam IDs that should be protected from student changes.
     * Omitted by the self-service endpoint.
     */
    private List<Long> lockedExamIds;
}
