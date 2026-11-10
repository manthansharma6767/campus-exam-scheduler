package com.manthan.campusexamscheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Maps a student to the absentee exams they are eligible to appear for.
 * Regular exams do not need a mapping and remain visible to the whole cohort.
 */
@Entity
@Table(
        name = "student_absentee_exams",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_absentee_exam",
                columnNames = {"student_id", "exam_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAbsenteeExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_absentee_exam_id")
    private Long studentAbsenteeExamId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    @PrePersist
    void ensureAssignedAt() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
