package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.*;
import com.manthan.campusexamscheduler.service.StudentService;
import com.manthan.campusexamscheduler.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    public ResponseEntity<SubjectResponse> createSubject(
            @Valid @RequestBody SubjectRequest request) throws RuntimeException {

        return new ResponseEntity<>(subjectService.createSubject(request) , HttpStatus.CREATED);
    }

    @GetMapping()
    public ResponseEntity<List<SubjectResponse>> findAllSubjects() {
        return new ResponseEntity<>(subjectService.getAllSubjects() , HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponse> adminById(@PathVariable Long id ) {
        return new ResponseEntity<>(subjectService.getSubjectById(id) , HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectResponse> updateSubject(@PathVariable Long id , @RequestBody SubjectRequest request) {

        return new ResponseEntity<>(subjectService.updateSubject(id , request),HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {

        subjectService.deleteSubject(id);

        return ResponseEntity.noContent().build();
    }
}
