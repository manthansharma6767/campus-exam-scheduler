package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.*;
import com.manthan.campusexamscheduler.service.StudentService;
import com.manthan.campusexamscheduler.service.StudentImportService;
import com.manthan.campusexamscheduler.service.StudentVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentImportService studentImportService;
    private final StudentVerificationService studentVerificationService;

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportPreviewResponse> previewStudents(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(studentImportService.previewStudents(file));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentImportResponse> importStudents(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(studentImportService.importStudents(file));
    }

    @PostMapping(value = "/dob-backfill/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentDobBackfillPreviewResponse> previewStudentDobBackfill(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(studentImportService.previewStudentDobBackfill(file));
    }

    @PostMapping(value = "/dob-backfill", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentDobBackfillResponse> backfillStudentDob(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(studentImportService.backfillStudentDob(file));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody StudentRequest request) throws RuntimeException {

        return new ResponseEntity<>(studentService.registerStudent(request) , HttpStatus.CREATED);
    }
    @GetMapping()
    public ResponseEntity<List<StudentResponse>> findAllStudents() {
        return new ResponseEntity<>(studentService.getAllStudents() , HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> studentById(@PathVariable Long id ) {
        return new ResponseEntity<>(studentService.getStudentById(id) , HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable Long id , @Valid @RequestBody StudentRequest request) {

        return new ResponseEntity<>(studentService.updateStudent(id , request),HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {

        studentService.deleteStudent(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{enrollmentNumber}/verify")
    public ResponseEntity<StudentVerificationResponse> verifyStudent(
            @PathVariable String enrollmentNumber,
            @Valid @RequestBody StudentVerificationRequest request) {

        return ResponseEntity.ok(studentVerificationService.verify(
                enrollmentNumber,
                request.getDateOfBirth()
        ));
    }

    @GetMapping("/{enrollmentNumber}/schedule")
    public ResponseEntity<List<StudentScheduleResponse>> getStudentSchedule(
            @PathVariable String enrollmentNumber) {

        return ResponseEntity.ok(
                studentService.getStudentSchedule(enrollmentNumber)
        );
    }

    @GetMapping("/{enrollmentNumber}/absentee-eligibility")
    public ResponseEntity<List<StudentAbsenteeExamOptionResponse>> getMyAbsenteeExamOptions(
            @PathVariable String enrollmentNumber,
            @RequestHeader(value = "X-Student-Verification", required = false)
            String verificationToken) {

        studentVerificationService.requireVerifiedEnrollment(enrollmentNumber, verificationToken);
        return ResponseEntity.ok(
                studentService.getAbsenteeExamOptionsForEnrollment(enrollmentNumber)
        );
    }

    @PutMapping("/{enrollmentNumber}/absentee-eligibility")
    public ResponseEntity<List<StudentAbsenteeExamOptionResponse>> updateMyAbsenteeEligibility(
            @PathVariable String enrollmentNumber,
            @RequestHeader(value = "X-Student-Verification", required = false)
            String verificationToken,
            @Valid @RequestBody StudentAbsenteeEligibilityRequest request) {

        String verifiedEnrollment = studentVerificationService
                .requireVerifiedEnrollment(enrollmentNumber, verificationToken);
        return ResponseEntity.ok(
                studentService.updateAbsenteeEligibilityForEnrollment(
                        verifiedEnrollment,
                        request.getExamIds()
                )
        );
    }

    @GetMapping("/{studentId}/absentee-exams")
    public ResponseEntity<List<StudentAbsenteeExamOptionResponse>> getAbsenteeExamOptions(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(studentService.getAbsenteeExamOptions(studentId));
    }

    @PutMapping("/{studentId}/absentee-exams")
    public ResponseEntity<List<StudentAbsenteeExamOptionResponse>> updateAbsenteeEligibility(
            @PathVariable Long studentId,
            @Valid @RequestBody StudentAbsenteeEligibilityRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                studentService.updateAbsenteeEligibility(
                        studentId,
                        request.getExamIds(),
                        request.getLockedExamIds(),
                        authentication == null ? "admin" : authentication.getName()
                )
        );
    }
}
