package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.AdministratorRequest;
import com.manthan.campusexamscheduler.dto.AdministratorResponse;
import com.manthan.campusexamscheduler.dto.AuthenticationRequest;
import com.manthan.campusexamscheduler.dto.AuthenticationResponse;
import com.manthan.campusexamscheduler.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody AuthenticationRequest request) {

        return ResponseEntity.ok(authenticationService.login(request));
    }

    /**
     * One-time bootstrap endpoint to create the first administrator.
     * Returns 400 if an admin already exists, so it cannot be used to hijack
     * a running system.
     */
    @PostMapping("/register")
    public ResponseEntity<AdministratorResponse> bootstrapAdmin(
            @RequestHeader(value = "X-Admin-Setup-Token", required = false) String setupToken,
            @Valid @RequestBody AdministratorRequest request) {

        return new ResponseEntity<>(authenticationService.bootstrapAdmin(request, setupToken), HttpStatus.CREATED);
    }
}
