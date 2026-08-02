package com.manthan.campusexamscheduler.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResponse {

    private Long examId;

    private LocalDate examDate;

    private LocalTime examTime;

    private String roomNumber;

    private Long subjectId;

    private String subjectCode;

    private String subjectName;
}