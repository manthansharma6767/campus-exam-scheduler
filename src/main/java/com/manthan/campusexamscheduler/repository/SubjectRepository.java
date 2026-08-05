package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @Query("""
            SELECT s
            FROM Subject s
            JOIN FETCH s.department
            """)
    List<Subject> findAllWithDepartment();

    @Query("""
            SELECT s
            FROM Subject s
            JOIN FETCH s.department
            WHERE s.subjectId = :id
            """)
    Optional<Subject> findByIdWithDepartment(@Param("id") Long id);

    Optional<Subject> findBySubjectCode(String subjectCode);

    List<Subject> findBySubjectNameContainingIgnoreCase(String subjectName);

    List<Subject> findBySemester(Integer semester);

    List<Subject> findByDepartment(Department department);

    List<Subject> findBySemesterAndDepartment(
            Integer semester,
            Department department
    );

    boolean existsBySubjectCode(String subjectCode);
}