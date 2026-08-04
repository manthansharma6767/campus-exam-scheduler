package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.*;
import com.manthan.campusexamscheduler.service.DepartmentService;
import com.manthan.campusexamscheduler.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.manthan.campusexamscheduler.dto.StudentScheduleResponse;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

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
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable Long id , @RequestBody StudentRequest request) {

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
