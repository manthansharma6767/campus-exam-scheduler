package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.SeatAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SeatAssignmentRepository extends JpaRepository<SeatAssignment, Long> {

    Optional<SeatAssignment> findBySeatingPlan_SeatingPlanIdAndRollNumberIgnoreCase(Long seatingPlanId, String rollNumber);

    long countBySeatingPlan_SeatingPlanId(Long seatingPlanId);

    @Query("select count(distinct assignment.roomNumber) from SeatAssignment assignment where assignment.seatingPlan.seatingPlanId = :planId")
    long countDistinctRooms(@Param("planId") Long planId);

    void deleteAllBySeatingPlan_SeatingPlanId(Long seatingPlanId);
}
