package com.manthan.campusexamscheduler.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long studentId;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 100)
    @Column(
            nullable = false,
            unique = true,
            length = 100
    )
    private String email;

    @NotBlank
    @Size(min = 8, max = 255)
    @Column(nullable = false, length = 255)
    private String password;

    @NotBlank
    @Column(nullable = false, length = 255, unique = true)
    private String enrollmentNumber;

    @NotNull
    @Min(1)
    @Max(8)
    @Column(nullable = false)
    private Integer semester;

    /**
     * Optional for existing records, but required before a student can use
     * enrollment + date-of-birth self-service verification.
     */
    @Past
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "dob_updated_at")
    private LocalDateTime dobUpdatedAt;

    @Column(name = "dob_updated_by", length = 255)
    private String dobUpdatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
