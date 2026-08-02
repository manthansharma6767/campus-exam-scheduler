package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.ExamRequest;
import com.manthan.campusexamscheduler.dto.ExamResponse;
import com.manthan.campusexamscheduler.dto.StudentRequest;
import com.manthan.campusexamscheduler.dto.StudentResponse;
import com.manthan.campusexamscheduler.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
    public ResponseEntity<ExamResponse> createExam(
            @Valid @RequestBody ExamRequest request) throws RuntimeException {

        return new ResponseEntity<>(examService.createExam(request) , HttpStatus.CREATED);
    }

    @GetMapping()
    public ResponseEntity<List<ExamResponse>> findAllExams() {
        return new ResponseEntity<>(examService.getAllExams() , HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamResponse> examById(@PathVariable Long id ) {
        return new ResponseEntity<>(examService.getExamById(id) , HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExamResponse> updateExam(@PathVariable Long id , @RequestBody ExamRequest request) {

        return new ResponseEntity<>(examService.updateExam(id , request),HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {

        examService.deleteExamById(id);

        return ResponseEntity.noContent().build();
    }
}
