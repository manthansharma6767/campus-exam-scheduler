package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.*;
import com.manthan.campusexamscheduler.service.StudentService;
import com.manthan.campusexamscheduler.service.StudentImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentImportService studentImportService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentImportResponse> importStudents(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(studentImportService.importStudents(file));
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

    @GetMapping("/{enrollmentNumber}/schedule")
    public ResponseEntity<List<StudentScheduleResponse>> getStudentSchedule(
            @PathVariable String enrollmentNumber) {

        return ResponseEntity.ok(
                studentService.getStudentSchedule(enrollmentNumber)
        );
    }
}
