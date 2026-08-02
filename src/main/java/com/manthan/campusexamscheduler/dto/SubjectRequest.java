package com.manthan.campusexamscheduler.dto;


import com.manthan.campusexamscheduler.entity.Department;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectRequest {

    @NotBlank
    @Size(max = 20)
    private String subjectCode;

    @NotBlank
    @Size(min = 2, max = 100)
    private String subjectName;

    @NotNull
    @Min(1)
    @Max(8)
    private Integer semester;

    @NotNull
    private Long departmentId;
}
