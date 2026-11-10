package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.Exam;
import com.manthan.campusexamscheduler.entity.ExamType;
import com.manthan.campusexamscheduler.entity.StudentAbsenteeExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAbsenteeExamRepository extends JpaRepository<StudentAbsenteeExam, Long> {

    List<StudentAbsenteeExam> findByStudent_StudentId(Long studentId);

    void deleteByStudent_StudentId(Long studentId);

    void deleteByExam_ExamId(Long examId);

    @Query("""
            SELECT sae.exam.examId
            FROM StudentAbsenteeExam sae
            WHERE sae.student.studentId = :studentId
            """)
    List<Long> findExamIdsByStudentId(@Param("studentId") Long studentId);

    @Query("""
            SELECT e
            FROM StudentAbsenteeExam sae
            JOIN sae.exam e
            JOIN FETCH e.subject s
            WHERE sae.student.studentId = :studentId
              AND s.department.departmentId = :departmentId
              AND s.semester = :semester
              AND e.examType = :examType
            ORDER BY e.examDate ASC, e.examTime ASC, s.subjectCode ASC
            """)
    List<Exam> findEligibleAbsenteeExams(
            @Param("studentId") Long studentId,
            @Param("departmentId") Long departmentId,
            @Param("semester") Integer semester,
            @Param("examType") ExamType examType);
}
