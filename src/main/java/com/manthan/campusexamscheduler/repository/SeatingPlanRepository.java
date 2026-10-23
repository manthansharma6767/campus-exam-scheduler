package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.SeatingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatingPlanRepository extends JpaRepository<SeatingPlan, Long> {

    List<SeatingPlan> findAllByOrderByImportedAtDesc();

    Optional<SeatingPlan> findFirstByTitleAndDepartmentNameAndSemester(String title, String departmentName, Integer semester);
}
