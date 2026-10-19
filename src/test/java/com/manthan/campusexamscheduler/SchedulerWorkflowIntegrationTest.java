package com.manthan.campusexamscheduler;

import com.manthan.campusexamscheduler.dto.AdministratorRequest;
import com.manthan.campusexamscheduler.dto.AuthenticationRequest;
import com.manthan.campusexamscheduler.dto.DepartmentRequest;
import com.manthan.campusexamscheduler.dto.ExamRequest;
import com.manthan.campusexamscheduler.dto.StudentRequest;
import com.manthan.campusexamscheduler.dto.SubjectRequest;
import com.manthan.campusexamscheduler.exception.DuplicateResourceException;
import com.manthan.campusexamscheduler.service.AuthenticationService;
import com.manthan.campusexamscheduler.service.DepartmentService;
import com.manthan.campusexamscheduler.service.ExamService;
import com.manthan.campusexamscheduler.service.StudentService;
import com.manthan.campusexamscheduler.service.SubjectService;
import com.manthan.campusexamscheduler.service.TimetableImportService;
import com.manthan.campusexamscheduler.util.JwtService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ExamService examService;

    @Autowired
    private TimetableImportService timetableImportService;

    @Autowired
    private JwtService jwtService;

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
        studentService.registerStudent(StudentRequest.builder()
                .name("Test Student")
                .email("student@example.com")
                .password("StudentPass123")
                .enrollmentNumber("RB2101")
                .semester(5)
                .departmentId(department.getDepartmentId())
                .build());

        ExamRequest firstExam = examRequest(subject.getSubjectId(), LocalDate.of(2026, 11, 12), LocalTime.of(9, 0));
        examService.createExam(firstExam);
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
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Timetable");
            Row header = sheet.createRow(0);
            String[] headers = {"Subject Code", "Date", "Time", "Building", "Room"};
            for (int column = 0; column < headers.length; column++) {
                header.createCell(column).setCellValue(headers[column]);
            }
            Row exam = sheet.createRow(1);
            exam.createCell(0).setCellValue("CS101");
            exam.createCell(1).setCellValue("2026-11-13");
            exam.createCell(2).setCellValue("13:30");
            exam.createCell(3).setCellValue("Science Block");
            exam.createCell(4).setCellValue("B-110");
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
