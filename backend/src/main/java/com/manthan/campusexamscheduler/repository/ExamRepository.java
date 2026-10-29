package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;

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
            WHERE (:query IS NULL
                OR LOWER(s.subjectCode) LIKE CONCAT('%', LOWER(:query), '%')
                OR LOWER(s.subjectName) LIKE CONCAT('%', LOWER(:query), '%')
                OR LOWER(e.building) LIKE CONCAT('%', LOWER(:query), '%')
                OR LOWER(e.roomNumber) LIKE CONCAT('%', LOWER(:query), '%'))
                AND (:semester IS NULL OR s.semester = :semester)
                AND (:examDate IS NULL OR e.examDate = :examDate)
            ORDER BY e.examDate ASC, e.examTime ASC, s.subjectCode ASC
            """)
    List<Exam> searchPublicExams(
            @Param("query") String query,
            @Param("semester") Integer semester,
            @Param("examDate") LocalDate examDate);

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

    boolean existsBySubject_SubjectIdAndExamDateAndExamTime(Long subjectId, LocalDate examDate, LocalTime examTime);

    boolean existsBySubject_SubjectIdAndExamDateAndExamTimeAndExamIdNot(
            Long subjectId, LocalDate examDate, LocalTime examTime, Long examId);

    boolean existsByBuildingAndRoomNumberAndExamDateAndExamTime(
            String building, String roomNumber, LocalDate examDate, LocalTime examTime);

    boolean existsByBuildingAndRoomNumberAndExamDateAndExamTimeAndExamIdNot(
            String building, String roomNumber, LocalDate examDate, LocalTime examTime, Long examId);

    @Query("""
            SELECT e
            FROM Exam e
            JOIN FETCH e.subject s
            JOIN FETCH s.department d
            WHERE e.examDate = :examDate
              AND e.examTime = :examTime
              AND s.department.departmentId = :departmentId
              AND s.semester = :semester
            """)
    List<Exam> findConflictingStudentExams(
            @Param("examDate") LocalDate examDate,
            @Param("examTime") LocalTime examTime,
            @Param("departmentId") Long departmentId,
            @Param("semester") Integer semester);
}

