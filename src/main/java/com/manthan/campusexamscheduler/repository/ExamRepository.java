package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("""
            SELECT e
            FROM Exam e
            JOIN FETCH e.subject s
            JOIN FETCH s.department
            """)
    List<Exam> findAllWithSubject();

    @Query("""
            SELECT e
            FROM Exam e
            JOIN FETCH e.subject s
            JOIN FETCH s.department
            WHERE e.examId = :id
            """)
    Optional<Exam> findByIdWithSubject(@Param("id") Long id);

    List<Exam> findBySubject_DepartmentAndSubject_SemesterOrderByExamDateAscExamTimeAsc(
            Department department,
            Integer semester
    );


}