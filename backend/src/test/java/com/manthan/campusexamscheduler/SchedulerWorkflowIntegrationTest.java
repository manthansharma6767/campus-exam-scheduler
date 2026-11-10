package com.manthan.campusexamscheduler;

import com.manthan.campusexamscheduler.dto.AdministratorRequest;
import com.manthan.campusexamscheduler.dto.AuthenticationRequest;
import com.manthan.campusexamscheduler.dto.DepartmentRequest;
import com.manthan.campusexamscheduler.dto.ExamRequest;
import com.manthan.campusexamscheduler.dto.ImportPreviewResponse;
import com.manthan.campusexamscheduler.dto.ImportRowError;
import com.manthan.campusexamscheduler.dto.StudentRequest;
import com.manthan.campusexamscheduler.dto.SubjectRequest;
import com.manthan.campusexamscheduler.entity.ExamType;
import com.manthan.campusexamscheduler.exception.DuplicateResourceException;
import com.manthan.campusexamscheduler.exception.ImportValidationException;
import com.manthan.campusexamscheduler.exception.ScheduleConflictException;
import com.manthan.campusexamscheduler.repository.AdministratorRepository;
import com.manthan.campusexamscheduler.service.AdministratorService;
import com.manthan.campusexamscheduler.service.AuthenticationService;
import com.manthan.campusexamscheduler.service.DepartmentService;
import com.manthan.campusexamscheduler.service.ExamService;
import com.manthan.campusexamscheduler.service.StudentImportService;
import com.manthan.campusexamscheduler.service.StudentService;
import com.manthan.campusexamscheduler.service.SubjectService;
import com.manthan.campusexamscheduler.service.SeatingPlanService;
import com.manthan.campusexamscheduler.service.TimetableImportService;
import com.manthan.campusexamscheduler.util.JwtService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class SchedulerWorkflowIntegrationTest {
    private static final String RUN_ID = UUID.randomUUID().toString()
            .replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    private static final String RUN_SUFFIX = RUN_ID.toLowerCase(Locale.ROOT);
    private static final String ADMIN_EMAIL = "admin." + RUN_SUFFIX + "@example.test";
    private static final String STUDENT_EMAIL = "student." + RUN_SUFFIX + "@example.test";
    private static final String PREVIEW_EMAIL = "valid.preview." + RUN_SUFFIX + "@example.test";
    private static final String BULK_EMAIL = "bulk.one." + RUN_SUFFIX + "@example.test";
    private static final String EXCEL_EMAIL = "excel.import." + RUN_SUFFIX + "@example.test";
    private static final String DELETE_EMAIL = "delete.workflow." + RUN_SUFFIX + "@example.test";
    private static final String ABSENTEE_EMAIL = "absentee." + RUN_SUFFIX + "@example.test";
    private static final String STUDENT_ROLL = "RB-" + RUN_ID + "-01";
    private static final String ABSENTEE_ROLL = "RB-" + RUN_ID + "-AB";
    private static final String PREVIEW_ROLL = "RB-" + RUN_ID + "-P1";
    private static final String INVALID_PREVIEW_ROLL = "RB-" + RUN_ID + "-P2";
    private static final String BULK_ROLL = "RB-" + RUN_ID + "-I1";
    private static final String NOT_SAVED_ROLL = "RB-" + RUN_ID + "-I2";
    private static final String INVALID_SEMESTER_ROLL = "RB-" + RUN_ID + "-I3";
    private static final String EXCEL_ROLL = "RB-" + RUN_ID + "-X1";
    private static final String DELETE_ROLL = "RB-" + RUN_ID + "-D1";
    private static final String CS_CODE = testCode("CS1");
    private static final String CF1_CODE = testCode("CF1");
    private static final String CF2_CODE = testCode("CF2");
    private static final String VAL_CODE = testCode("VAL");
    private static final String DEL_CODE = testCode("DEL");
    private static final String MATRIX_CODE_1 = RUN_ID + "TH0501";
    private static final String MATRIX_CODE_2 = RUN_ID + "TH0502";
    private static final String ABSENTEE_CODE = testCode("ABS");
    private static final String ABSENTEE_CODE_2 = testCode("AB2");
    private static final String SEATING_TITLE = "Sessional Test 1 " + RUN_ID;
    private static final String SEATING_DEPARTMENT = "Department of Computer Science " + RUN_ID;

    @DynamicPropertySource
    static void configureMySqlTestDatabase(DynamicPropertyRegistry registry) {
        MySqlTestProperties.register(registry);
    }

    private static String testName(String base) {
        return base + " " + RUN_ID;
    }

    private static String testCode(String suffix) {
        return "T" + RUN_ID + suffix;
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AdministratorService administratorService;

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentImportService studentImportService;

    @Autowired
    private ExamService examService;

    @Autowired
    private SeatingPlanService seatingPlanService;

    @Autowired
    private TimetableImportService timetableImportService;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void configureMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void adminEndpointsReturnUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentImportPreviewReportsAllErrorsWithoutSavingOrExposingPasswords() throws Exception {
        var department = departmentService.createDepartment(
                DepartmentRequest.builder().departmentName(testName("Bulk Preview Department")).build());
        MockMultipartFile invalidRoster = csvFile("students.csv", """
                Name,Email,Password,Roll Number,Semester,Department
                Valid Student,%s,SecretPass123,%s,5,%s
                Invalid Student,not-an-email,short,%s,5,%s
                """.formatted(PREVIEW_EMAIL, PREVIEW_ROLL, department.getDepartmentName(),
                INVALID_PREVIEW_ROLL, department.getDepartmentName()));

        ImportPreviewResponse preview = studentImportService.previewStudents(invalidRoster);

        assertEquals(1, preview.validRows());
        assertEquals(1, preview.errors().size());
        assertFalse(preview.canImport());
        assertFalse(preview.rows().toString().contains("SecretPass123"));
        assertFalse(studentService.getAllStudents().stream()
                .anyMatch(student -> student.getEmail().equals(PREVIEW_EMAIL)));
        assertThrows(ImportValidationException.class,
                () -> studentImportService.importStudents(invalidRoster));
        assertFalse(studentService.getAllStudents().stream()
                .anyMatch(student -> student.getEmail().equals(PREVIEW_EMAIL)));

        MockMultipartFile validRoster = csvFile("students.csv", """
                Name,Email,Password,Roll Number,Semester,Department
                Valid Student,%s,SecretPass123,%s,5,%s
                """.formatted(PREVIEW_EMAIL, PREVIEW_ROLL, department.getDepartmentName()));
        mockMvc.perform(multipart("/api/students/import/preview").file(validRoster).with(user("test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canImport").value(true))
                .andExpect(jsonPath("$.rows[0].values.Password").doesNotExist());
        ImportPreviewResponse validPreview = studentImportService.previewStudents(validRoster);
        assertTrue(validPreview.canImport());
        assertEquals(1, studentImportService.importStudents(validRoster).getImported());
        assertTrue(studentService.getAllStudents().stream()
                .anyMatch(student -> student.getEmail().equals(PREVIEW_EMAIL)));
    }

    @Test
    void timetablePreviewCollectsRowErrorsAndScheduleConflictsWithoutSaving() throws Exception {
        var department = departmentService.createDepartment(
                DepartmentRequest.builder().departmentName(testName("Schedule Conflict Department")).build());
        var firstSubject = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode(CF1_CODE).subjectName("Conflict One").semester(3)
                .departmentId(department.getDepartmentId()).build());
        var secondSubject = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode(CF2_CODE).subjectName("Conflict Two").semester(3)
                .departmentId(department.getDepartmentId()).build());
        long examsBefore = examService.getAllExams().size();
        MockMultipartFile timetable = new MockMultipartFile("file", "conflicts.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                spreadsheetBytes(new String[][] {
                        {CF1_CODE, "2098-12-31", "09:00-11:00", "Conflict Hall", "101"},
                        {CF2_CODE, "2098-12-31", "10:30-12:00", "Conflict Hall", "101"},
                        {"UNKNOWN", "not-a-date", "09:00", "Other Hall", "202"}
                }));

        ImportPreviewResponse preview = timetableImportService.previewTimetable(timetable);

        assertEquals(2, preview.validRows());
        assertEquals(1, preview.errors().size());
        assertEquals(2, preview.conflicts().size());
        assertEquals("09:00–11:00", preview.rows().get(0).values().get("Time"));
        assertFalse(preview.canImport());
        assertEquals(examsBefore, examService.getAllExams().size());
        mockMvc.perform(multipart("/api/exams/import/preview").file(timetable).with(user("test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canImport").value(false))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.conflicts.length()").value(2));
        mockMvc.perform(multipart("/api/exams/import").file(timetable).with(user("test-admin")))
                .andExpect(status().is(HttpStatus.UNPROCESSABLE_CONTENT.value()))
                .andExpect(jsonPath("$.canImport").value(false));
        assertThrows(ImportValidationException.class,
                () -> timetableImportService.importTimetable(timetable));
        assertEquals(examsBefore, examService.getAllExams().size());

        ExamRequest firstExam = examRequest(firstSubject.getSubjectId(),
                LocalDate.of(2098, 12, 30), LocalTime.of(9, 0));
        examService.createExam(firstExam);
        ExamRequest cohortConflict = ExamRequest.builder()
                .examDate(LocalDate.of(2098, 12, 30))
                .examTime(LocalTime.of(10, 30))
                .examEndTime(LocalTime.of(12, 0))
                .building("Another Hall")
                .roomNumber("202")
                .subjectId(secondSubject.getSubjectId())
                .build();
        assertThrows(ScheduleConflictException.class, () -> examService.createExam(cohortConflict));
        assertThrows(IllegalArgumentException.class, () -> examService.createExam(ExamRequest.builder()
                .examDate(LocalDate.of(2098, 12, 30))
                .examTime(LocalTime.of(9, 0))
                .examEndTime(LocalTime.of(9, 0))
                .roomNumber("303")
                .building("Other Hall")
                .subjectId(firstSubject.getSubjectId())
                .build()));
    }

    @Test
    void timetableImportPreviewRejectsMissingOrOversizedRoomDetailsBeforeSaving() throws Exception {
        var department = departmentService.createDepartment(
                DepartmentRequest.builder().departmentName(testName("Timetable Field Validation Department")).build());
        subjectService.createSubject(SubjectRequest.builder()
                .subjectCode(VAL_CODE)
                .subjectName("Validation Test")
                .semester(4)
                .departmentId(department.getDepartmentId())
                .build());
        long examsBefore = examService.getAllExams().size();
        String oversizedBuilding = "B".repeat(101);
        String oversizedRoom = "R".repeat(21);
        MockMultipartFile timetable = new MockMultipartFile("file", "invalid-rooms.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                spreadsheetBytes(new String[][] {
                        {VAL_CODE, "2098-12-31", "09:00", "", "101"},
                        {VAL_CODE, "2098-12-31", "10:00", "Main Building", ""},
                        {VAL_CODE, "2098-12-31", "11:00", oversizedBuilding, "102"},
                        {VAL_CODE, "2098-12-31", "12:00", "Main Building", oversizedRoom}
                }));

        ImportPreviewResponse preview = timetableImportService.previewTimetable(timetable);

        // Missing venues are intentionally accepted and defaulted to "Not assigned";
        // oversized field values remain hard validation errors.
        assertEquals(2, preview.validRows());
        assertEquals(2, preview.errors().size());
        assertTrue(preview.rows().stream().allMatch(row ->
                "Not assigned".equals(row.values().get("Building"))
                        || "Not assigned".equals(row.values().get("Room"))));
        assertTrue(preview.errors().toString().contains("building"));
        assertTrue(preview.errors().toString().contains("room"));
        assertFalse(preview.canImport());
        assertThrows(ImportValidationException.class,
                () -> timetableImportService.importTimetable(timetable));
        assertEquals(examsBefore, examService.getAllExams().size());
    }

    @Test
    void setupLoginDataEntryStudentScheduleAndTimetableImportWorkTogether() throws Exception {
        AdministratorRequest admin = AdministratorRequest.builder()
                .name("Test Admin")
                .email(ADMIN_EMAIL)
                .password("AdminPass123")
                .build();
        String setupToken = MySqlTestProperties.adminSetupToken();
        assertThrows(IllegalArgumentException.class,
                () -> authenticationService.bootstrapAdmin(admin, "wrong-token"));
        if (administratorRepository.count() == 0) {
            authenticationService.bootstrapAdmin(admin, setupToken);
        } else {
            // An existing administrator is preserved; login is still verified with a
            // temporary test administrator that is rolled back with this transaction.
            assertThrows(IllegalArgumentException.class,
                    () -> authenticationService.bootstrapAdmin(admin, setupToken));
            administratorService.registerAdmin(admin);
        }
        assertThrows(IllegalArgumentException.class,
                () -> authenticationService.bootstrapAdmin(admin, setupToken));

        var login = authenticationService.login(AuthenticationRequest.builder()
                .email(admin.getEmail())
                .password(admin.getPassword())
                .build());
        assertEquals(admin.getEmail(), jwtService.extractUsername(login.getToken()));

        var department = departmentService.createDepartment(
                DepartmentRequest.builder().departmentName(testName("Computer Science")).build());
        var subject = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode(CS_CODE)
                .subjectName("Data Structures")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        var updatedSubject = subjectService.updateSubject(subject.getSubjectId(), SubjectRequest.builder()
                .subjectCode(CS_CODE)
                .subjectName("Data Structures Updated")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        assertEquals("Data Structures Updated", updatedSubject.getSubjectName());
        var student = studentService.registerStudent(StudentRequest.builder()
                .name("Test Student")
                .email(STUDENT_EMAIL)
                .password("StudentPass123")
                .enrollmentNumber(STUDENT_ROLL)
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        var updatedStudent = studentService.updateStudent(student.getStudentId(), StudentRequest.builder()
                .name("Test Student Updated")
                .email(STUDENT_EMAIL)
                .enrollmentNumber(STUDENT_ROLL)
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        assertEquals("Test Student Updated", updatedStudent.getName());

        ExamRequest firstExam = examRequest(subject.getSubjectId(), LocalDate.of(2026, 11, 12), LocalTime.of(9, 0));
        var createdExam = examService.createExam(firstExam);
        var updatedExam = examService.updateExam(createdExam.getExamId(), ExamRequest.builder()
                .examDate(firstExam.getExamDate())
                .examTime(firstExam.getExamTime())
                .roomNumber("A-206")
                .subjectId(subject.getSubjectId())
                .building("Updated Hall")
                .build());
        assertEquals("Updated Hall", updatedExam.getBuilding());
        assertEquals(1, studentService.getStudentSchedule(STUDENT_ROLL).size());
        mockMvc.perform(get("/api/exams/search")
                        .param("query", CS_CODE)
                        .param("semester", "5")
                        .param("date", "2026-11-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectCode").value(CS_CODE));
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isUnauthorized());
        assertThrows(DuplicateResourceException.class, () -> examService.createExam(firstExam));

        MockMultipartFile timetable = new MockMultipartFile(
                "file", "timetable.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                spreadsheetBytes());
        var firstImport = timetableImportService.importTimetable(timetable);
        assertEquals(1, firstImport.getImported());
        assertEquals(0, firstImport.getSkippedDuplicates());

        var duplicateImport = timetableImportService.importTimetable(timetable);
        assertEquals(0, duplicateImport.getImported());
        assertEquals(1, duplicateImport.getSkippedDuplicates());

        var schedule = studentService.getStudentSchedule(STUDENT_ROLL);
        assertEquals(2, schedule.size());
        assertTrue(schedule.stream().allMatch(exam -> CS_CODE.equals(exam.getSubjectCode())));

        var matrixSubjectOne = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode(MATRIX_CODE_1)
                .subjectName("Algorithms")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        var matrixSubjectTwo = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode(MATRIX_CODE_2)
                .subjectName("Operating Systems")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        MockMultipartFile matrixPdf = new MockMultipartFile(
                "file", "matrix-timetable.pdf", MediaType.APPLICATION_PDF_VALUE,
                matrixPdfBytes());

        mockMvc.perform(multipart("/api/exams/import")
                        .file(matrixPdf)
                        .header("Authorization", "Bearer " + login.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(4))
                .andExpect(jsonPath("$.skippedDuplicates").value(1));

        mockMvc.perform(multipart("/api/exams/import")
                        .file(matrixPdf)
                        .header("Authorization", "Bearer " + login.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.skippedDuplicates").value(5));

        var savedMatrixExams = examService.getAllExams().stream()
                .filter(exam -> List.of(MATRIX_CODE_1, MATRIX_CODE_2).contains(exam.getSubjectCode()))
                .toList();
        assertEquals(4, savedMatrixExams.size());
        assertTrue(savedMatrixExams.stream().allMatch(exam ->
                "Not assigned".equals(exam.getBuilding()) && "Not assigned".equals(exam.getRoomNumber())));
        assertTrue(savedMatrixExams.stream().anyMatch(exam ->
                exam.getSubjectId().equals(matrixSubjectOne.getSubjectId())
                        && exam.getExamDate().equals(LocalDate.of(2026, 11, 18))
                        && exam.getExamTime().equals(LocalTime.of(14, 0))));
        assertTrue(savedMatrixExams.stream().anyMatch(exam ->
                exam.getSubjectId().equals(matrixSubjectTwo.getSubjectId())
                        && exam.getExamDate().equals(LocalDate.of(2026, 11, 20))
                        && exam.getExamTime().equals(LocalTime.of(9, 30))));
    }

    @Test
    void importsAndLooksUpSeatsFromPositionedPdfAndReplacesDuplicatePlan() throws Exception {
        MockMultipartFile seatingPdf = new MockMultipartFile(
                "file", "5th-sem-seating.pdf", MediaType.APPLICATION_PDF_VALUE, seatingPdfBytes());

        var imported = seatingPlanService.importPdf(seatingPdf, null);

        assertEquals(SEATING_TITLE, imported.getPlan().getTitle());
        assertEquals(SEATING_DEPARTMENT, imported.getPlan().getDepartmentName());
        assertEquals(5, imported.getPlan().getSemester());
        assertEquals(4, imported.getImportedSeats());
        assertEquals(1, imported.getPlan().getRoomCount());

        var seat = seatingPlanService.findSeat(imported.getPlan().getSeatingPlanId(), "a1-4");
        assertEquals("A1-4", seat.getRollNumber());
        assertEquals("A1", seat.getSection());
        assertEquals("DT-101", seat.getRoomNumber());
        assertEquals(1, seat.getRowNumber());
        assertEquals(2, seat.getColumnNumber());

        var replaced = seatingPlanService.importPdf(seatingPdf, null);
        assertTrue(replaced.isReplacedExistingPlan());
        assertEquals(imported.getPlan().getSeatingPlanId(), replaced.getPlan().getSeatingPlanId());
        assertEquals(4, replaced.getPlan().getSeatCount());
    }

    @Test
    void importsStudentCsvSkipsDuplicatesAndRejectsInvalidRowsAtomically() throws Exception {
        String departmentName = testName("Student Import Test Department");
        departmentService.createDepartment(
                DepartmentRequest.builder().departmentName(departmentName).build());
        String header = "Name,Email,Password,Roll Number,Semester,Department\r\n";
        String validRow = "\"Doe, Jane\"," + BULK_EMAIL + ",ImportPass123," + BULK_ROLL + ",4,"
                + departmentName + "\r\n";
        MockMultipartFile studentFile = new MockMultipartFile(
                "file", "students.csv", "text/csv",
                (header + validRow).getBytes(StandardCharsets.UTF_8));

        var firstImport = studentImportService.importStudents(studentFile);
        assertEquals(1, firstImport.getImported());
        assertEquals(0, firstImport.getSkippedDuplicates());
        assertTrue(studentService.getAllStudents().stream()
                .anyMatch(student -> BULK_ROLL.equals(student.getEnrollmentNumber())
                        && "Doe, Jane".equals(student.getName())));

        var repeatedImport = studentImportService.importStudents(studentFile);
        assertEquals(0, repeatedImport.getImported());
        assertEquals(1, repeatedImport.getSkippedDuplicates());

        String invalidRows = header
                + "Not Saved,not-saved." + RUN_SUFFIX + "@example.test,ImportPass123," + NOT_SAVED_ROLL + ",4,"
                + departmentName + "\r\n"
                + "Invalid Semester,invalid-semester." + RUN_SUFFIX + "@example.test,ImportPass123,"
                + INVALID_SEMESTER_ROLL + ",9,"
                + departmentName + "\r\n";
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "invalid-students.csv", "text/csv",
                invalidRows.getBytes(StandardCharsets.UTF_8));
        ImportValidationException validationError = assertThrows(
                ImportValidationException.class,
                () -> studentImportService.importStudents(invalidFile));
        assertEquals(1, validationError.getPreview().errors().size());
        ImportRowError rowError = validationError.getPreview().errors().get(0);
        assertEquals(3, rowError.rowNumber());
        assertTrue(rowError.message().contains("semester"));
        assertTrue(studentService.getAllStudents().stream()
                .noneMatch(student -> NOT_SAVED_ROLL.equals(student.getEnrollmentNumber())));
    }

    @Test
    void importsStudentXlsxRosterAndSkipsRepeatedRollNumbers() throws Exception {
        String departmentName = testName("Student Excel Import Test Department");
        departmentService.createDepartment(
                DepartmentRequest.builder().departmentName(departmentName).build());

        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            String[] headers = {"Name", "Email", "Password", "Roll Number", "Semester", "Department"};
            for (int column = 0; column < headers.length; column++) {
                header.createCell(column).setCellValue(headers[column]);
            }
            Row student = sheet.createRow(1);
            student.createCell(0).setCellValue("Excel Import Student");
            student.createCell(1).setCellValue(EXCEL_EMAIL);
            student.createCell(2).setCellValue("ExcelImportPass123");
            student.createCell(3).setCellValue(EXCEL_ROLL);
            student.createCell(4).setCellValue(6);
            student.createCell(5).setCellValue(departmentName);
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        MockMultipartFile studentFile = new MockMultipartFile(
                "file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes);

        ImportPreviewResponse preview = studentImportService.previewStudents(studentFile);
        assertTrue(preview.canImport());
        assertEquals(1, preview.validRows());
        assertFalse(preview.rows().toString().contains("ExcelImportPass123"));

        var imported = studentImportService.importStudents(studentFile);
        assertEquals(1, imported.getImported());
        assertEquals(0, imported.getSkippedDuplicates());
        assertTrue(studentService.getAllStudents().stream()
                .anyMatch(savedStudent -> EXCEL_ROLL.equals(savedStudent.getEnrollmentNumber())
                        && "Excel Import Student".equals(savedStudent.getName())));

        var repeatedImport = studentImportService.importStudents(studentFile);
        assertEquals(0, repeatedImport.getImported());
        assertEquals(1, repeatedImport.getSkippedDuplicates());
    }

    @Test
    void studentCanSelfDeclareAbsenceAndOnlyAbsenteePaperAppears() throws Exception {
        var department = departmentService.createDepartment(
                DepartmentRequest.builder().departmentName(testName("Absentee Eligibility Department")).build());
        var missedSubject = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode(ABSENTEE_CODE)
                .subjectName("Cloud Computing Absentee Test")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        var attendedSubject = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode(ABSENTEE_CODE_2)
                .subjectName("Attended Paper Test")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        var student = studentService.registerStudent(StudentRequest.builder()
                .name("Absentee Test Student")
                .email(ABSENTEE_EMAIL)
                .password("StudentPass123")
                .enrollmentNumber(ABSENTEE_ROLL)
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());

        examService.createExam(ExamRequest.builder()
                .examDate(LocalDate.of(2026, 11, 3))
                .examTime(LocalTime.of(11, 0))
                .examEndTime(LocalTime.of(12, 0))
                .roomNumber("A-101")
                .building("Main Building")
                .subjectId(missedSubject.getSubjectId())
                .examType(ExamType.REGULAR)
                .build());
        examService.createExam(ExamRequest.builder()
                .examDate(LocalDate.of(2026, 11, 4))
                .examTime(LocalTime.of(11, 0))
                .examEndTime(LocalTime.of(12, 0))
                .roomNumber("A-102")
                .building("Main Building")
                .subjectId(attendedSubject.getSubjectId())
                .examType(ExamType.REGULAR)
                .build());
        var absenteeExam = examService.createExam(ExamRequest.builder()
                .examDate(LocalDate.of(2026, 11, 13))
                .examTime(LocalTime.of(15, 0))
                .examEndTime(LocalTime.of(16, 0))
                .roomNumber("Not assigned")
                .building("Not assigned")
                .subjectId(missedSubject.getSubjectId())
                .examType(ExamType.ABSENTEE)
                .build());

        assertEquals(2, studentService.getStudentSchedule(ABSENTEE_ROLL).size());

        mockMvc.perform(get("/api/students/{enrollmentNumber}/absentee-eligibility", ABSENTEE_ROLL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subjectCode").value(ABSENTEE_CODE))
                .andExpect(jsonPath("$[0].assigned").value(false));

        mockMvc.perform(put("/api/students/{enrollmentNumber}/absentee-eligibility", ABSENTEE_ROLL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examIds\":[" + absenteeExam.getExamId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assigned").value(true));

        var schedule = studentService.getStudentSchedule(ABSENTEE_ROLL);
        assertEquals(2, schedule.size());
        assertTrue(schedule.stream().anyMatch(exam ->
                ABSENTEE_CODE_2.equals(exam.getSubjectCode())
                        && exam.getExamType() == ExamType.REGULAR));
        assertTrue(schedule.stream().anyMatch(exam ->
                ABSENTEE_CODE.equals(exam.getSubjectCode())
                        && exam.getExamType() == ExamType.ABSENTEE));
        assertFalse(schedule.stream().anyMatch(exam ->
                ABSENTEE_CODE.equals(exam.getSubjectCode())
                        && exam.getExamType() == ExamType.REGULAR));

        mockMvc.perform(get("/api/students/{enrollmentNumber}/schedule", ABSENTEE_ROLL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].subjectCode").value(ABSENTEE_CODE_2))
                .andExpect(jsonPath("$[1].subjectCode").value(ABSENTEE_CODE))
                .andExpect(jsonPath("$[1].examType").value("ABSENTEE"));

        mockMvc.perform(get("/api/students/{studentId}/absentee-exams", student.getStudentId())
                        .with(user("test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assigned").value(true));

        mockMvc.perform(put("/api/students/{enrollmentNumber}/absentee-eligibility", ABSENTEE_ROLL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assigned").value(false));
        var restoredSchedule = studentService.getStudentSchedule(ABSENTEE_ROLL);
        assertEquals(2, restoredSchedule.size());
        assertTrue(restoredSchedule.stream().allMatch(exam -> exam.getExamType() == ExamType.REGULAR));
    }

    @Test
    void deletesDepartmentSubjectStudentAndExamRecords() throws Exception {
        var department = departmentService.createDepartment(
                DepartmentRequest.builder().departmentName(testName("Delete Workflow Test Department")).build());
        var subject = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode(DEL_CODE)
                .subjectName("Deletion Test")
                .semester(2)
                .departmentId(department.getDepartmentId())
                .build());
        var student = studentService.registerStudent(StudentRequest.builder()
                .name("Delete Workflow Student")
                .email(DELETE_EMAIL)
                .password("TestOnlyPass123")
                .enrollmentNumber(DELETE_ROLL)
                .semester(2)
                .departmentId(department.getDepartmentId())
                .build());
        var exam = examService.createExam(examRequest(
                subject.getSubjectId(), LocalDate.of(2027, 5, 11), LocalTime.of(10, 0)));

        examService.deleteExamById(exam.getExamId());
        studentService.deleteStudent(student.getStudentId());
        subjectService.deleteSubject(subject.getSubjectId());
        departmentService.deleteDepartment(department.getDepartmentId());

        assertTrue(examService.getAllExams().stream()
                .noneMatch(savedExam -> savedExam.getExamId().equals(exam.getExamId())));
        assertTrue(studentService.getAllStudents().stream()
                .noneMatch(savedStudent -> savedStudent.getStudentId().equals(student.getStudentId())));
        assertTrue(subjectService.getAllSubjects().stream()
                .noneMatch(savedSubject -> savedSubject.getSubjectId().equals(subject.getSubjectId())));
        assertTrue(departmentService.getAllDepartments().stream()
                .noneMatch(savedDepartment -> savedDepartment.getDepartmentId().equals(department.getDepartmentId())));
    }

    private ExamRequest examRequest(Long subjectId, LocalDate date, LocalTime time) {
        return ExamRequest.builder()
                .examDate(date)
                .examTime(time)
                .examEndTime(time.plusHours(2))
                .roomNumber("A-205")
                .subjectId(subjectId)
                .building("Main Building")
                .build();
    }

    private byte[] spreadsheetBytes() throws Exception {
        return spreadsheetBytes(new String[][] {{CS_CODE, "2026-11-13", "13:30", "Science Block", "B-110"}});
    }

    private byte[] spreadsheetBytes(String[][] exams) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Timetable");
            Row header = sheet.createRow(0);
            String[] headers = {"Subject Code", "Date", "Time", "Building", "Room"};
            for (int column = 0; column < headers.length; column++) {
                header.createCell(column).setCellValue(headers[column]);
            }
            for (int index = 0; index < exams.length; index++) {
                Row exam = sheet.createRow(index + 1);
                for (int column = 0; column < exams[index].length; column++) {
                    exam.createCell(column).setCellValue(exams[index][column]);
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private MockMultipartFile csvFile(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private byte[] matrixPdfBytes() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                draw(content, font, page, "Time: 14:00", 30, 45);
                draw(content, font, page, "18-Nov-26", 50, 65);
                draw(content, font, page, "20-Nov-26", 250, 65);
                draw(content, font, page, "Course Code", 50, 85);
                draw(content, font, page, "Course Code", 250, 85);
                draw(content, font, page, MATRIX_CODE_1, 50, 105);
                draw(content, font, page, MATRIX_CODE_2, 250, 105);
                draw(content, font, page, MATRIX_CODE_1 + "*", 50, 120);

                draw(content, font, page, "Time: 09:30", 30, 155);
                draw(content, font, page, "18-Nov-26", 50, 175);
                draw(content, font, page, "20-Nov-26", 250, 175);
                draw(content, font, page, "Course Code", 50, 195);
                draw(content, font, page, "Course Code", 250, 195);
                draw(content, font, page, MATRIX_CODE_1, 50, 215);
                draw(content, font, page, MATRIX_CODE_2, 250, 215);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] seatingPdfBytes() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                draw(content, font, page, SEATING_DEPARTMENT, 35, 60);
                draw(content, font, page, "5th Semester", 35, 80);
                draw(content, font, page, SEATING_TITLE, 35, 100);
                draw(content, font, page, "Class Room No: DT-101", 35, 130);
                draw(content, font, page, "A1-1", 80, 200);
                draw(content, font, page, "A1-2", 230, 200);
                draw(content, font, page, "A1-3", 80, 230);
                draw(content, font, page, "A1-4", 230, 230);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private void draw(PDPageContentStream content, PDType1Font font, PDPage page, String text, float x, float yFromTop)
            throws Exception {
        content.beginText();
        content.setFont(font, 9);
        content.newLineAtOffset(x, page.getMediaBox().getHeight() - yFromTop);
        content.showText(text);
        content.endText();
    }
}
