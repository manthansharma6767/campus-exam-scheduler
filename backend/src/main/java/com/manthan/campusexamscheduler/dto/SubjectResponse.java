package com.manthan.campusexamscheduler.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResponse {
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer semester;
    private Long departmentId;
    private String departmentName;
}
