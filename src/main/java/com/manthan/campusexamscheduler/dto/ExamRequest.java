package com.manthan.campusexamscheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamRequest {

    @NotNull
    private LocalDate examDate;

    @NotNull
    private LocalTime examTime;

    @NotBlank
    @Size(max = 20)
    private String roomNumber;

    @NotNull
    private Long subjectId;

    @NotBlank
    @Size(max = 100)
    private String building;
}
