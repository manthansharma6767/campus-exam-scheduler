package com.manthan.campusexamscheduler;

import com.manthan.campusexamscheduler.dto.DepartmentRequest;
import com.manthan.campusexamscheduler.dto.StudentRequest;
import com.manthan.campusexamscheduler.entity.Student;
import com.manthan.campusexamscheduler.exception.StudentDobBackfillValidationException;
import com.manthan.campusexamscheduler.repository.StudentRepository;
import com.manthan.campusexamscheduler.service.DepartmentService;
import com.manthan.campusexamscheduler.service.StudentImportService;
import com.manthan.campusexamscheduler.service.StudentService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class StudentDobBackfillIntegrationTest {

    private static final String RUN_ID = UUID.randomUUID().toString()
            .replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    private static final String RUN_SUFFIX = RUN_ID.toLowerCase(Locale.ROOT);

    @DynamicPropertySource
    static void configureMySqlTestDatabase(DynamicPropertyRegistry registry) {
        MySqlTestProperties.register(registry);
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentImportService studentImportService;

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void configureMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void conditionalRepositoryUpdateWritesAuditFieldsAndNeverOverwrites() throws Exception {
        String enrollment = registerStudent("repo", null);
        Student student = studentRepository.findByEnrollmentNumber(enrollment).orElseThrow();
        LocalDateTime updatedAt = LocalDateTime.of(2026, 11, 17, 9, 30);

        assertEquals(1, studentRepository.setDateOfBirthIfMissing(
                student.getStudentId(), LocalDate.of(2005, 8, 24),
                updatedAt, "admin@test"));

        Student saved = studentRepository.findById(student.getStudentId()).orElseThrow();
        assertEquals(LocalDate.of(2005, 8, 24), saved.getDateOfBirth());
        assertEquals(updatedAt, saved.getDobUpdatedAt());
        assertEquals("admin@test", saved.getDobUpdatedBy());

        assertEquals(0, studentRepository.setDateOfBirthIfMissing(
                student.getStudentId(), LocalDate.of(2004, 1, 15),
                updatedAt.plusMinutes(1), "other-admin"));

        Student unchanged = studentRepository.findById(student.getStudentId()).orElseThrow();
        assertEquals(LocalDate.of(2005, 8, 24), unchanged.getDateOfBirth());
        assertEquals(updatedAt, unchanged.getDobUpdatedAt());
        assertEquals("admin@test", unchanged.getDobUpdatedBy());
    }

    @Test
    void unmatchedEnrollmentNumbersBlockPreviewAndApply() throws Exception {
        String enrollment = registerStudent("unmatched", null);
        MockMultipartFile file = csv("""
                Enrollment Number,Date of Birth
                %s,2005-08-24
                UNKNOWN-%s,2004-01-01
                """.formatted(enrollment, RUN_ID));

        var preview = studentImportService.previewStudentDobBackfill(file, false);
        assertFalse(preview.canApply());
        assertEquals(1, preview.notFound());
        assertEquals(1, preview.missingDobRows());
        assertTrue(preview.message().contains("unmatched enrollment"));

        assertThrows(StudentDobBackfillValidationException.class,
                () -> studentImportService.backfillStudentDob(
                        file, false, true, "admin@test"));
        Student stored = studentRepository.findByEnrollmentNumber(enrollment).orElseThrow();
        assertNull(stored.getDateOfBirth());

        mockMvc.perform(multipart("/api/students/dob-backfill/preview")
                        .file(file)
                        .with(user("test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canApply").value(false))
                .andExpect(jsonPath("$.notFound").value(1));
    }

    @Test
    void overwriteModeRequiresConfirmationAndAuditsTheChange() throws Exception {
        String enrollment = registerStudent("overwrite", LocalDate.of(2004, 1, 15));
        MockMultipartFile file = csv("""
                Enrollment Number,Date of Birth
                %s,2006-07-27
                """.formatted(enrollment));

        var preview = studentImportService.previewStudentDobBackfill(file, true);
        assertTrue(preview.canApply());
        assertTrue(preview.overwriteExisting());
        assertEquals(1, preview.existingDobRows());
        assertEquals(1, preview.eligibleRows());
        assertEquals("Will overwrite existing DOB",
                preview.rows().get(0).values().get("Result"));

        assertThrows(IllegalArgumentException.class,
                () -> studentImportService.backfillStudentDob(
                        file, true, false, "admin@test"));

        var result = studentImportService.backfillStudentDob(
                file, true, true, "admin@test");
        assertEquals(1, result.updated());
        assertEquals(1, result.overwritten());
        assertEquals(0, result.alreadyPopulated());

        Student stored = studentRepository.findByEnrollmentNumber(enrollment).orElseThrow();
        assertEquals(LocalDate.of(2006, 7, 27), stored.getDateOfBirth());
        assertNotNull(stored.getDobUpdatedAt());
        assertEquals("admin@test", stored.getDobUpdatedBy());
    }

    @Test
    void excelBackfillPathUpdatesMissingDates() throws Exception {
        String enrollment = registerStudent("excel", null);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "student-dob-backfill.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                dobWorkbook(enrollment, "2005-09-12"));

        var preview = studentImportService.previewStudentDobBackfill(file, false);
        assertTrue(preview.canApply());
        assertEquals(1, preview.missingDobRows());

        var result = studentImportService.backfillStudentDob(
                file, false, true, "admin@test");
        assertEquals(1, result.updated());
        assertEquals(LocalDate.of(2005, 9, 12),
                studentRepository.findByEnrollmentNumber(enrollment)
                        .orElseThrow().getDateOfBirth());
    }

    @Test
    void endpointRequiresAuthenticationAndConfirmation() throws Exception {
        String enrollment = registerStudent("api", null);
        MockMultipartFile file = csv("""
                Enrollment Number,Date of Birth
                %s,2005-08-24
                """.formatted(enrollment));

        mockMvc.perform(multipart("/api/students/dob-backfill/preview").file(file))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(multipart("/api/students/dob-backfill/preview")
                        .file(file)
                        .with(user("test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canApply").value(true))
                .andExpect(jsonPath("$.missingDobRows").value(1));

        mockMvc.perform(multipart("/api/students/dob-backfill")
                        .file(file)
                        .with(user("test-admin")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/students/dob-backfill")
                        .file(file)
                        .param("confirm", "true")
                        .with(user("test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.overwritten").value(0));

        Student stored = studentRepository.findByEnrollmentNumber(enrollment).orElseThrow();
        assertEquals(LocalDate.of(2005, 8, 24), stored.getDateOfBirth());
        assertEquals("test-admin", stored.getDobUpdatedBy());
    }

    private String registerStudent(String suffix, LocalDate dateOfBirth) throws Exception {
        String departmentName = "DOB Backfill " + suffix + " Department " + RUN_ID;
        var department = departmentService.createDepartment(
                DepartmentRequest.builder().departmentName(departmentName).build());
        String enrollment = "DOB-" + RUN_ID + "-" + suffix.toUpperCase(Locale.ROOT);
        studentService.registerStudent(StudentRequest.builder()
                .name("DOB Backfill " + suffix + " Student")
                .email("dob.backfill." + suffix + "." + RUN_SUFFIX + "@example.test")
                .password("StudentPass123")
                .enrollmentNumber(enrollment)
                .semester(5)
                .dateOfBirth(dateOfBirth)
                .departmentId(department.getDepartmentId())
                .build());
        return enrollment;
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile(
                "file",
                "student-dob-backfill.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] dobWorkbook(String enrollment, String dateOfBirth) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("DOB Backfill");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Enrollment Number");
            header.createCell(1).setCellValue("Date of Birth");
            Row student = sheet.createRow(1);
            student.createCell(0).setCellValue(enrollment);
            student.createCell(1).setCellValue(dateOfBirth);
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
