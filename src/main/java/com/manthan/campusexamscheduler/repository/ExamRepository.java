package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findBySubjectSemester(Integer semester);

    List<Exam> findBySubjectDepartment(Department department);

    List<Exam> findBySubjectSemesterAndSubjectDepartment(
            Integer semester,
            Department department
    );

}
