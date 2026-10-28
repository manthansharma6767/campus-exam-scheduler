package com.manthan.campusexamscheduler.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_plan_roll_number", columnNames = {"seating_plan_id", "roll_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seating_plan_id", nullable = false)
    private SeatingPlan seatingPlan;

    @Column(name = "roll_number", nullable = false, length = 20)
    private String rollNumber;

    @Column(name = "section_name", length = 10)
    private String section;

    @Column(name = "room_number", nullable = false, length = 30)
    private String roomNumber;

    @Column(name = "`row_number`", nullable = false)
    private Integer rowNumber;

    @Column(name = "column_number", nullable = false)
    private Integer columnNumber;

    @Column(name = "source_page", nullable = false)
    private Integer sourcePage;
}
