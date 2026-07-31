package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Student;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByEnrollmentNumber(String enrollmentNumber);

    List<Student> findBySemester(Integer semester);

    List<Student> findByDepartment(Department department);

    List<Student> findBySemesterAndDepartment(Integer semester,
                                              Department department);

    boolean existsByEmail(String email);

    boolean existsByEnrollmentNumber(String enrollmentNumber);
}
