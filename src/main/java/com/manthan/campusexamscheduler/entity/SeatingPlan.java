package com.manthan.campusexamscheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "seating_plans", uniqueConstraints = {
        @UniqueConstraint(name = "uk_seating_plan_identity", columnNames = {"plan_title", "department_name", "semester"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatingPlanId;

    @Column(name = "plan_title", nullable = false, length = 200)
    private String title;

    @Column(name = "department_name", nullable = false, length = 150)
    private String departmentName;

    @Column(nullable = false)
    private Integer semester;

    @Column(nullable = false)
    private Instant importedAt;

}
