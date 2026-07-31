package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.Administrator;
import com.manthan.campusexamscheduler.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByDepartmentName(String departmentName);

    boolean existsByDepartmentName(String departmentName);

}
