package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findBySubjectCode(String subjectCode);

    List<Subject> findBySubjectNameContainingIgnoreCase(String subjectName);

    List<Subject> findBySemester(Integer semester);

    List<Subject> findByDepartment(Department department);

    List<Subject> findBySemesterAndDepartment(Integer semester,
                                              Department department);

    boolean existsBySubjectCode(String subjectCode);

}