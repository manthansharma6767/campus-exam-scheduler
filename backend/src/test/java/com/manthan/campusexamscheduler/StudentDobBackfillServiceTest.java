package com.manthan.campusexamscheduler;

import com.manthan.campusexamscheduler.entity.Student;
import com.manthan.campusexamscheduler.exception.StudentDobBackfillValidationException;
import com.manthan.campusexamscheduler.repository.DepartmentRepository;
import com.manthan.campusexamscheduler.repository.StudentRepository;
import com.manthan.campusexamscheduler.service.StudentImportService;
import com.manthan.campusexamscheduler.entity.Department;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;

class StudentDobBackfillServiceTest {

    @Test
    void previewsMissingExistingAndUnknownStudentDates() {
        StudentRepository students = mock(StudentRepository.class);
        StudentImportService service = service(students);
        Student missingDob = student(1L, null);
        Student existingDob = student(2L, LocalDate.of(2004, 2, 17));
        when(students.findByEnrollmentNumberIgnoreCase("MISSING-DOB"))
                .thenReturn(Optional.of(missingDob));
        when(students.findByEnrollmentNumberIgnoreCase("HAS-DOB"))
                .thenReturn(Optional.of(existingDob));
        when(students.findByEnrollmentNumberIgnoreCase("UNKNOWN"))
                .thenReturn(Optional.empty());

        var preview = service.previewStudentDobBackfill(csv(
                "Enrollment Number,Date of Birth\n"
                        + "MISSING-DOB,2005-08-24\n"
                        + "HAS-DOB,2003-01-02\n"
                        + "UNKNOWN,2004-06-11\n"));

        assertEquals(1, preview.eligibleRows());
        assertEquals(1, preview.alreadyPopulated());
        assertEquals(1, preview.notFound());
        assertTrue(preview.canApply());
        assertTrue(preview.message().contains("preserved 1 existing date"));
        assertTrue(preview.message().contains("skipped 1 unmatched enrollment"));
        assertEquals("Will backfill", preview.rows().get(0).values().get("Result"));
        assertEquals("DOB already set; unchanged", preview.rows().get(1).values().get("Result"));
        assertEquals("Student not found", preview.rows().get(2).values().get("Result"));
    }

    @Test
    void appliesOnlyMissingDatesAndDoesNotOverwriteExistingDates() {
        StudentRepository students = mock(StudentRepository.class);
        StudentImportService service = service(students);
        Student missingDob = student(1L, null);
        Student existingDob = student(2L, LocalDate.of(2004, 2, 17));
        when(students.findByEnrollmentNumberIgnoreCase("MISSING-DOB"))
                .thenReturn(Optional.of(missingDob));
        when(students.findByEnrollmentNumberIgnoreCase("HAS-DOB"))
                .thenReturn(Optional.of(existingDob));
        when(students.setDateOfBirthIfMissing(1L, LocalDate.of(2005, 8, 24))).thenReturn(1);

        var result = service.backfillStudentDob(csv(
                "Enrollment Number,Date of Birth\n"
                        + "MISSING-DOB,2005-08-24\n"
                        + "HAS-DOB,2003-01-02\n"));

        assertEquals(1, result.updated());
        assertEquals(1, result.alreadyPopulated());
        assertEquals(0, result.notFound());
        verify(students).setDateOfBirthIfMissing(1L, LocalDate.of(2005, 8, 24));
        verify(students, never()).setDateOfBirthIfMissing(2L, LocalDate.of(2003, 1, 2));
    }

    @Test
    void rejectsWholeFileWhenAnyDateIsInvalid() {
        StudentRepository students = mock(StudentRepository.class);
        StudentImportService service = service(students);
        when(students.findByEnrollmentNumberIgnoreCase("MISSING-DOB"))
                .thenReturn(Optional.of(student(1L, null)));

        var preview = service.previewStudentDobBackfill(csv(
                "Enrollment Number,Date of Birth\n"
                        + "MISSING-DOB,2005-08-24\n"
                        + "BAD-DATE,2099-01-01\n"));

        assertFalse(preview.canApply());
        assertEquals(1, preview.errors().size());
        StudentDobBackfillValidationException exception = assertThrows(
                StudentDobBackfillValidationException.class,
                () -> service.backfillStudentDob(csv(
                        "Enrollment Number,Date of Birth\n"
                                + "MISSING-DOB,2005-08-24\n"
                                + "BAD-DATE,2099-01-01\n")));
        assertEquals(1, exception.getPreview().errors().size());
        verify(students, never()).setDateOfBirthIfMissing(
                1L, LocalDate.of(2005, 8, 24));
    }

    @Test
    void reportsConcurrentDobUpdatesAsPreservedInsteadOfOverwriting() {
        StudentRepository students = mock(StudentRepository.class);
        StudentImportService service = service(students);
        when(students.findByEnrollmentNumberIgnoreCase("MISSING-DOB"))
                .thenReturn(Optional.of(student(1L, null)));
        when(students.setDateOfBirthIfMissing(1L, LocalDate.of(2005, 8, 24))).thenReturn(0);

        var result = service.backfillStudentDob(csv(
                "Enrollment Number,Date of Birth\nMISSING-DOB,2005-08-24\n"));

        assertEquals(0, result.updated());
        assertEquals(1, result.alreadyPopulated());
    }

    @Test
    void keepsExistingStudentRosterImportWorking() {
        StudentRepository students = mock(StudentRepository.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Department department = mock(Department.class);
        when(departments.findByDepartmentNameIgnoreCase("Computer Science"))
                .thenReturn(Optional.of(department));
        when(department.getDepartmentName()).thenReturn("Computer Science");
        when(students.existsByEmail("one@example.test")).thenReturn(false);
        when(students.existsByEnrollmentNumber("ROLL-1")).thenReturn(false);
        when(passwordEncoder.encode("securepass1")).thenReturn("hashed-password");
        StudentImportService service = new StudentImportService(students, departments, passwordEncoder);

        var response = service.importStudents(new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                ("Name,Email,Password,Roll Number,Semester,Department\n"
                        + "Student One,one@example.test,securepass1,ROLL-1,2,Computer Science\n")
                        .getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, response.getImported());
        verify(students).saveAll(anyList());
    }

    private StudentImportService service(StudentRepository students) {
        return new StudentImportService(
                students, mock(DepartmentRepository.class), mock(PasswordEncoder.class));
    }

    private Student student(Long id, LocalDate dateOfBirth) {
        return Student.builder()
                .studentId(id)
                .enrollmentNumber("TEST-" + id)
                .dateOfBirth(dateOfBirth)
                .build();
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile(
                "file",
                "student-dob-backfill.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
