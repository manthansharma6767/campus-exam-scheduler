package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByEnrollmentNumber(String enrollmentNumber);

    Optional<Student> findByEnrollmentNumberIgnoreCase(String enrollmentNumber);

    List<Student> findBySemester(Integer semester);

    List<Student> findByDepartment(Department department);

    List<Student> findBySemesterAndDepartment(Integer semester,
                                              Department department);

    boolean existsByEmail(String email);

    boolean existsByEnrollmentNumber(String enrollmentNumber);

    boolean existsByEmailAndStudentIdNot(String email, Long studentId);

    boolean existsByEnrollmentNumberAndStudentIdNot(String enrollmentNumber, Long studentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Student student
            SET student.dateOfBirth = :dateOfBirth,
                student.dobUpdatedAt = :updatedAt,
                student.dobUpdatedBy = :updatedBy
            WHERE student.studentId = :studentId
              AND student.dateOfBirth IS NULL
            """)
    int setDateOfBirthIfMissing(
            @Param("studentId") Long studentId,
            @Param("dateOfBirth") LocalDate dateOfBirth,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") String updatedBy);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Student student
            SET student.dateOfBirth = :dateOfBirth,
                student.dobUpdatedAt = :updatedAt,
                student.dobUpdatedBy = :updatedBy
            WHERE student.studentId = :studentId
            """)
    int overwriteDateOfBirth(
            @Param("studentId") Long studentId,
            @Param("dateOfBirth") LocalDate dateOfBirth,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") String updatedBy);
}
