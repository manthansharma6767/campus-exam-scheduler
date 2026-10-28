package com.manthan.campusexamscheduler.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "exams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long examId;

    @NotNull
    @Column(nullable = false)
    private LocalDate examDate;

    @NotNull
    @Column(nullable = false)
    private LocalTime examTime;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String roomNumber;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

}

