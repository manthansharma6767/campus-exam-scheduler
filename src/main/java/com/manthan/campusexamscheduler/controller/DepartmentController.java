package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.DepartmentRequest;
import com.manthan.campusexamscheduler.dto.DepartmentResponse;
import com.manthan.campusexamscheduler.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentRequest request) throws Exception {

        return new ResponseEntity<>(departmentService.createDepartment(request) , HttpStatus.CREATED);
    }

    @GetMapping()
    public ResponseEntity<List<DepartmentResponse>> findAllDepartments() {
        return new ResponseEntity<>(departmentService.getAllDepartments() , HttpStatus.ACCEPTED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> findById(@PathVariable Long id) {

        return new ResponseEntity<>(departmentService.findById(id),HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id , @RequestBody DepartmentRequest request) {

        return new ResponseEntity<>(departmentService.updateDepartment(id , request),HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {

        departmentService.deleteDepartment(id);

        return ResponseEntity.noContent().build();
    }

}
