package com.manthan.campusexamscheduler.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentVerificationRequest {

    @NotNull
    private LocalDate dateOfBirth;
}
