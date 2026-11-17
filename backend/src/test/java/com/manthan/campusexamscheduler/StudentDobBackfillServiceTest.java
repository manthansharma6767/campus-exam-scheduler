package com.manthan.campusexamscheduler;

import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Student;
import com.manthan.campusexamscheduler.exception.StudentDobBackfillValidationException;
import com.manthan.campusexamscheduler.repository.DepartmentRepository;
import com.manthan.campusexamscheduler.repository.StudentRepository;
import com.manthan.campusexamscheduler.service.StudentImportService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                        + "UNKNOWN,2004-06-11\n"), false);

        assertEquals(3, preview.totalRows());
        assertEquals(1, preview.eligibleRows());
        assertEquals(1, preview.missingDobRows());
        assertEquals(1, preview.existingDobRows());
        assertEquals(1, preview.notFound());
        assertFalse(preview.canApply());
        assertTrue(preview.message().contains("unmatched enrollment"));
        assertEquals("Will backfill", preview.rows().get(0).values().get("Result"));
        assertEquals("DOB already set; unchanged", preview.rows().get(1).values().get("Result"));
        assertEquals("Student not found", preview.rows().get(2).values().get("Result"));
    }

    @Test
    void previewsOverwriteModeForExistingDates() {
        StudentRepository students = mock(StudentRepository.class);
        StudentImportService service = service(students);
        Student existingDob = student(2L, LocalDate.of(2004, 2, 17));
        when(students.findByEnrollmentNumberIgnoreCase("HAS-DOB"))
                .thenReturn(Optional.of(existingDob));

        var preview = service.previewStudentDobBackfill(csv(
                "Enrollment Number,Date of Birth\nHAS-DOB,2006-07-27\n"), true);

        assertTrue(preview.canApply());
        assertTrue(preview.overwriteExisting());
        assertEquals(1, preview.eligibleRows());
        assertEquals(1, preview.existingDobRows());
        assertEquals("Will overwrite existing DOB",
                preview.rows().get(0).values().get("Result"));
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
        when(students.setDateOfBirthIfMissing(
                eq(1L), eq(LocalDate.of(2005, 8, 24)),
                any(LocalDateTime.class), eq("admin@test"))).thenReturn(1);

        var result = service.backfillStudentDob(csv(
                "Enrollment Number,Date of Birth\n"
                        + "MISSING-DOB,2005-08-24\n"
                        + "HAS-DOB,2003-01-02\n"), false, true, "admin@test");

        assertEquals(1, result.updated());
        assertEquals(0, result.overwritten());
        assertEquals(1, result.alreadyPopulated());
        assertEquals(0, result.notFound());
        verify(students).setDateOfBirthIfMissing(
                eq(1L), eq(LocalDate.of(2005, 8, 24)),
                any(LocalDateTime.class), eq("admin@test"));
        verify(students, never()).overwriteDateOfBirth(
                any(), any(), any(), any());
    }

    @Test
    void unmatchedEnrollmentNumbersBlockTheWholeApply() {
        StudentRepository students = mock(StudentRepository.class);
        StudentImportService service = service(students);
        when(students.findByEnrollmentNumberIgnoreCase("MISSING-DOB"))
                .thenReturn(Optional.of(student(1L, null)));
        when(students.findByEnrollmentNumberIgnoreCase("UNKNOWN"))
                .thenReturn(Optional.empty());

        MockMultipartFile file = csv(
                "Enrollment Number,Date of Birth\n"
                        + "MISSING-DOB,2005-08-24\n"
                        + "UNKNOWN,2004-06-11\n");
        var preview = service.previewStudentDobBackfill(file, false);

        assertFalse(preview.canApply());
        assertEquals(1, preview.notFound());
        assertThrows(StudentDobBackfillValidationException.class,
                () -> service.backfillStudentDob(file, false, true, "admin@test"));
        verify(students, never()).setDateOfBirthIfMissing(
                any(), any(), any(), any());
        verify(students, never()).overwriteDateOfBirth(
                any(), any(), any(), any());
    }

    @Test
    void overwriteModeRequiresExplicitConfirmation() {
        StudentRepository students = mock(StudentRepository.class);
        StudentImportService service = service(students);
        Student existingDob = student(2L, LocalDate.of(2004, 1, 15));
        when(students.findByEnrollmentNumberIgnoreCase("HAS-DOB"))
                .thenReturn(Optional.of(existingDob));
        when(students.overwriteDateOfBirth(
                eq(2L), eq(LocalDate.of(2006, 7, 27)),
                any(LocalDateTime.class), eq("admin@test"))).thenReturn(1);

        MockMultipartFile file = csv(
                "Enrollment Number,Date of Birth\nHAS-DOB,2006-07-27\n");

        assertThrows(IllegalArgumentException.class,
                () -> service.backfillStudentDob(file, true, false, "admin@test"));
        var result = service.backfillStudentDob(file, true, true, "admin@test");

        assertEquals(1, result.updated());
        assertEquals(1, result.overwritten());
        assertEquals(0, result.alreadyPopulated());
        verify(students).overwriteDateOfBirth(
                eq(2L), eq(LocalDate.of(2006, 7, 27)),
                any(LocalDateTime.class), eq("admin@test"));
    }

    @Test
    void rejectsWholeFileWhenAnyDateIsInvalid() {
        StudentRepository students = mock(StudentRepository.class);
        StudentImportService service = service(students);
        when(students.findByEnrollmentNumberIgnoreCase("MISSING-DOB"))
                .thenReturn(Optional.of(student(1L, null)));

        MockMultipartFile file = csv(
                "Enrollment Number,Date of Birth\n"
                        + "MISSING-DOB,2005-08-24\n"
                        + "BAD-DATE,2099-01-01\n");
        var preview = service.previewStudentDobBackfill(file, false);

        assertFalse(preview.canApply());
        assertEquals(1, preview.errors().size());
        StudentDobBackfillValidationException exception = assertThrows(
                StudentDobBackfillValidationException.class,
                () -> service.backfillStudentDob(file, false, true, "admin@test"));
        assertEquals(1, exception.getPreview().errors().size());
        verify(students, never()).setDateOfBirthIfMissing(
                any(), any(), any(), any());
    }

    @Test
    void reportsConcurrentDobUpdatesAsPreservedInsteadOfOverwriting() {
        StudentRepository students = mock(StudentRepository.class);
        StudentImportService service = service(students);
        when(students.findByEnrollmentNumberIgnoreCase("MISSING-DOB"))
                .thenReturn(Optional.of(student(1L, null)));
        when(students.setDateOfBirthIfMissing(
                eq(1L), eq(LocalDate.of(2005, 8, 24)),
                any(LocalDateTime.class), eq("admin@test"))).thenReturn(0);

        var result = service.backfillStudentDob(csv(
                "Enrollment Number,Date of Birth\nMISSING-DOB,2005-08-24\n"),
                false, true, "admin@test");

        assertEquals(0, result.updated());
        assertEquals(0, result.overwritten());
        assertTrue(result.message().contains("another request"));
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
