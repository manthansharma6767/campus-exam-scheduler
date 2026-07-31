package com.manthan.campusexamscheduler.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponse {

    private Long studentId;

    private String name;

    private String email;

    private String enrollmentNumber;

    private Integer semester;

    private Long departmentId;

    private String departmentName;

}