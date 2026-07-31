package com.manthan.campusexamscheduler.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(min = 8, max = 255)
    private String password;

    @NotBlank
    @Size(max = 255)
    private String enrollmentNumber;

    @NotNull
    @Min(1)
    @Max(8)
    private Integer semester;

    @NotNull
    private Long departmentId;
}