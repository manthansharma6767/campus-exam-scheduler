package com.manthan.campusexamscheduler;

import com.manthan.campusexamscheduler.dto.AdministratorRequest;
import com.manthan.campusexamscheduler.dto.AuthenticationRequest;
import com.manthan.campusexamscheduler.dto.DepartmentRequest;
import com.manthan.campusexamscheduler.dto.ExamRequest;
import com.manthan.campusexamscheduler.dto.ImportPreviewResponse;
import com.manthan.campusexamscheduler.dto.StudentRequest;
import com.manthan.campusexamscheduler.dto.SubjectRequest;
import com.manthan.campusexamscheduler.exception.DuplicateResourceException;
import com.manthan.campusexamscheduler.exception.ImportValidationException;
import com.manthan.campusexamscheduler.exception.ScheduleConflictException;
import com.manthan.campusexamscheduler.service.AuthenticationService;
import com.manthan.campusexamscheduler.service.DepartmentService;
import com.manthan.campusexamscheduler.service.ExamService;
import com.manthan.campusexamscheduler.service.StudentService;
import com.manthan.campusexamscheduler.service.StudentImportService;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scheduler_workflow_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_SETUP_TOKEN=workflow-test-setup-token",
        "JWT_SECRET=integration-test-jwt-secret-with-at-least-32-characters"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SchedulerWorkflowIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AuthenticationService authenticationService;

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
                DepartmentRequest.builder().departmentName("Bulk Preview Department").build());
        MockMultipartFile invalidRoster = csvFile("students.csv", """
                Name,Email,Password,Roll Number,Semester,Department
                Valid Student,valid.preview@example.com,SecretPass123,RBPREVIEW1,5,%s
                Invalid Student,not-an-email,short,RBPREVIEW2,5,%s
                """.formatted(department.getDepartmentName(), department.getDepartmentName()));

        ImportPreviewResponse preview = studentImportService.previewStudents(invalidRoster);

        assertEquals(1, preview.validRows());
        assertEquals(1, preview.errors().size());
        assertFalse(preview.canImport());
        assertFalse(preview.rows().toString().contains("SecretPass123"));
        assertFalse(studentService.getAllStudents().stream()
                .anyMatch(student -> student.getEmail().equals("valid.preview@example.com")));
        assertThrows(ImportValidationException.class,
                () -> studentImportService.importStudents(invalidRoster));
        assertFalse(studentService.getAllStudents().stream()
                .anyMatch(student -> student.getEmail().equals("valid.preview@example.com")));

        MockMultipartFile validRoster = csvFile("students.csv", """
                Name,Email,Password,Roll Number,Semester,Department
                Valid Student,valid.preview@example.com,SecretPass123,RBPREVIEW1,5,%s
                """.formatted(department.getDepartmentName()));
        mockMvc.perform(multipart("/api/students/import/preview").file(validRoster).with(user("test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canImport").value(true))
                .andExpect(jsonPath("$.rows[0].values.Password").doesNotExist());
        ImportPreviewResponse validPreview = studentImportService.previewStudents(validRoster);
        assertTrue(validPreview.canImport());
        assertEquals(1, studentImportService.importStudents(validRoster).getImported());
        assertTrue(studentService.getAllStudents().stream()
                .anyMatch(student -> student.getEmail().equals("valid.preview@example.com")));
    }

    @Test
    void timetablePreviewCollectsRowErrorsAndScheduleConflictsWithoutSaving() throws Exception {
        var department = departmentService.createDepartment(
                DepartmentRequest.builder().departmentName("Schedule Conflict Department").build());
        var firstSubject = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode("CF101").subjectName("Conflict One").semester(3)
                .departmentId(department.getDepartmentId()).build());
        var secondSubject = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode("CF102").subjectName("Conflict Two").semester(3)
                .departmentId(department.getDepartmentId()).build());
        long examsBefore = examService.getAllExams().size();
        MockMultipartFile timetable = new MockMultipartFile("file", "conflicts.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                spreadsheetBytes(new String[][] {
                        {"CF101", "2098-12-31", "09:00", "Conflict Hall", "101"},
                        {"CF102", "2098-12-31", "09:00", "Conflict Hall", "101"},
                        {"UNKNOWN", "not-a-date", "09:00", "Other Hall", "202"}
                }));

        ImportPreviewResponse preview = timetableImportService.previewTimetable(timetable);

        assertEquals(2, preview.validRows());
        assertEquals(1, preview.errors().size());
        assertEquals(2, preview.conflicts().size());
        assertFalse(preview.canImport());
        assertEquals(examsBefore, examService.getAllExams().size());
        mockMvc.perform(multipart("/api/exams/import/preview").file(timetable).with(user("test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canImport").value(false))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.conflicts.length()").value(2));
        mockMvc.perform(multipart("/api/exams/import").file(timetable).with(user("test-admin")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.canImport").value(false));
        assertThrows(ImportValidationException.class,
                () -> timetableImportService.importTimetable(timetable));
        assertEquals(examsBefore, examService.getAllExams().size());

        ExamRequest firstExam = examRequest(firstSubject.getSubjectId(),
                LocalDate.of(2098, 12, 30), LocalTime.of(9, 0));
        examService.createExam(firstExam);
        ExamRequest cohortConflict = ExamRequest.builder()
                .examDate(LocalDate.of(2098, 12, 30))
                .examTime(LocalTime.of(9, 0))
                .building("Another Hall")
                .roomNumber("202")
                .subjectId(secondSubject.getSubjectId())
                .build();
        assertThrows(ScheduleConflictException.class, () -> examService.createExam(cohortConflict));
    }

    @Test
    void setupLoginDataEntryStudentScheduleAndTimetableImportWorkTogether() throws Exception {
        AdministratorRequest admin = AdministratorRequest.builder()
                .name("Test Admin")
                .email("admin@example.com")
                .password("AdminPass123")
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> authenticationService.bootstrapAdmin(admin, "wrong-token"));
        authenticationService.bootstrapAdmin(admin, "workflow-test-setup-token");
        assertThrows(IllegalArgumentException.class,
                () -> authenticationService.bootstrapAdmin(admin, "workflow-test-setup-token"));

        var login = authenticationService.login(AuthenticationRequest.builder()
                .email(admin.getEmail())
                .password(admin.getPassword())
                .build());
        assertEquals(admin.getEmail(), jwtService.extractUsername(login.getToken()));

        var department = departmentService.createDepartment(
                DepartmentRequest.builder().departmentName("Computer Science").build());
        var subject = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode("CS101")
                .subjectName("Data Structures")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        var updatedSubject = subjectService.updateSubject(subject.getSubjectId(), SubjectRequest.builder()
                .subjectCode("CS101")
                .subjectName("Data Structures Updated")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        assertEquals("Data Structures Updated", updatedSubject.getSubjectName());
        var student = studentService.registerStudent(StudentRequest.builder()
                .name("Test Student")
                .email("student@example.com")
                .password("StudentPass123")
                .enrollmentNumber("RB2101")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        var updatedStudent = studentService.updateStudent(student.getStudentId(), StudentRequest.builder()
                .name("Test Student Updated")
                .email("student@example.com")
                .enrollmentNumber("RB2101")
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
        assertEquals(1, studentService.getStudentSchedule("RB2101").size());
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

        var schedule = studentService.getStudentSchedule("RB2101");
        assertEquals(2, schedule.size());
        assertTrue(schedule.stream().allMatch(exam -> "CS101".equals(exam.getSubjectCode())));

        var matrixSubjectOne = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode("24CS01TH0501")
                .subjectName("Algorithms")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());
        var matrixSubjectTwo = subjectService.createSubject(SubjectRequest.builder()
                .subjectCode("24CS01TH0502")
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
                .filter(exam -> List.of("24CS01TH0501", "24CS01TH0502").contains(exam.getSubjectCode()))
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

        assertEquals("Sessional Test 1", imported.getPlan().getTitle());
        assertEquals("Department of Computer Science", imported.getPlan().getDepartmentName());
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

    private ExamRequest examRequest(Long subjectId, LocalDate date, LocalTime time) {
        return ExamRequest.builder()
                .examDate(date)
                .examTime(time)
                .roomNumber("A-205")
                .subjectId(subjectId)
                .building("Main Building")
                .build();
    }

    private byte[] spreadsheetBytes() throws Exception {
        return spreadsheetBytes(new String[][] {{"CS101", "2026-11-13", "13:30", "Science Block", "B-110"}});
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
                draw(content, font, page, "24CS01TH0501", 50, 105);
                draw(content, font, page, "24CS01TH0502", 250, 105);
                draw(content, font, page, "24CS01TH0501*", 50, 120);

                draw(content, font, page, "Time: 09:30", 30, 155);
                draw(content, font, page, "18-Nov-26", 50, 175);
                draw(content, font, page, "20-Nov-26", 250, 175);
                draw(content, font, page, "Course Code", 50, 195);
                draw(content, font, page, "Course Code", 250, 195);
                draw(content, font, page, "24CS01TH0501", 50, 215);
                draw(content, font, page, "24CS01TH0502", 250, 215);
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
                draw(content, font, page, "Department of Computer Science", 35, 60);
                draw(content, font, page, "5th Semester", 35, 80);
                draw(content, font, page, "Sessional Test 1", 35, 100);
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
