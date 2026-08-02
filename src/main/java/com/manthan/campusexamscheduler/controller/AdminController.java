package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.*;
import com.manthan.campusexamscheduler.service.AdministratorService;
import com.manthan.campusexamscheduler.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdministratorService administratorService;

    @PostMapping
    public ResponseEntity<AdministratorResponse> createAdmin(
            @Valid @RequestBody AdministratorRequest request) throws RuntimeException {

        return new ResponseEntity<>(administratorService.registerAdmin(request) , HttpStatus.CREATED);
    }

    @GetMapping()
    public ResponseEntity<List<AdministratorResponse>> findAllAdmins() {
        return new ResponseEntity<>(administratorService.getAllAdmins() , HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdministratorResponse> adminById(@PathVariable Long id ) {
        return new ResponseEntity<>(administratorService.getAdminById(id) , HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdministratorResponse> updateAdmin(@PathVariable Long id , @RequestBody AdministratorRequest request) {

        return new ResponseEntity<>(administratorService.updateAdmin(id , request),HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {

        administratorService.deleteAdmin(id);

        return ResponseEntity.noContent().build();
    }
}
