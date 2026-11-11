package com.manthan.campusexamscheduler.dto;

import com.manthan.campusexamscheduler.entity.AbsenteeAssignmentSource;
import com.manthan.campusexamscheduler.entity.ExamType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAbsenteeExamOptionResponse {

    private Long examId;

    private String subjectCode;

    private String subjectName;

    private LocalDate examDate;

    private LocalTime examTime;

    private LocalTime examEndTime;

    private String building;

    private String roomNumber;

    private ExamType examType;

    private boolean assigned;

    private boolean locked;

    private AbsenteeAssignmentSource source;

    private LocalDateTime updatedAt;

    private String updatedBy;
}
