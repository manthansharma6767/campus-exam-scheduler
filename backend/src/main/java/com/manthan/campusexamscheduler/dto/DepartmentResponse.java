package com.manthan.campusexamscheduler.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponse {

    private Long departmentId;

    private String departmentName;

}