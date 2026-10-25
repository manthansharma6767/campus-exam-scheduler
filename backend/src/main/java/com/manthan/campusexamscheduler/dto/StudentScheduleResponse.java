package com.manthan.campusexamscheduler.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentScheduleResponse {

    private String subjectCode;

    private String subjectName;

    private LocalDate examDate;

    private LocalTime examTime;

    private String building;

    private String roomNumber;
}
