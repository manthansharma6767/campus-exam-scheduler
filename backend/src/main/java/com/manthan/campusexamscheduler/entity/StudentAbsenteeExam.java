package com.manthan.campusexamscheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Maps a student to the absentee exams they are eligible to appear for.
 * Regular exams do not need a mapping and remain visible to the whole cohort.
 *
 * <p>Merge semantics:</p>
 * <ul>
 *   <li>An admin save replaces the complete mapping set for a student.</li>
 *   <li>A student save keeps every locked/admin-protected mapping and can only
 *       remove unlocked rows.</li>
 *   <li>{@code source} records who created or last claimed the row, while
 *       {@code updatedBy} records the last actor.</li>
 * </ul>
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AbsenteeAssignmentSource source = AbsenteeAssignmentSource.STUDENT;

    @Column(nullable = false)
    @Builder.Default
    private boolean locked = false;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    void ensureAuditFields() {
        LocalDateTime now = LocalDateTime.now();
        if (assignedAt == null) {
            assignedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (source == null) {
            source = AbsenteeAssignmentSource.STUDENT;
        }
    }

    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }
}
