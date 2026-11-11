package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.AdministratorRequest;
import com.manthan.campusexamscheduler.dto.AdministratorResponse;
import com.manthan.campusexamscheduler.dto.AuthenticationRequest;
import com.manthan.campusexamscheduler.dto.AuthenticationResponse;
import com.manthan.campusexamscheduler.dto.ChangePasswordRequest;
import com.manthan.campusexamscheduler.service.AdministratorService;
import com.manthan.campusexamscheduler.service.AuthenticationService;
import com.manthan.campusexamscheduler.service.RateLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AdministratorService administratorService;
    private final RateLimitService rateLimitService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody AuthenticationRequest request) {

        String rateLimitKey = loginKey(request.getEmail());
        rateLimitService.check(rateLimitKey);
        try {
            AuthenticationResponse response = authenticationService.login(request);
            rateLimitService.clear(rateLimitKey);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException exception) {
            rateLimitService.recordFailure(rateLimitKey);
            throw exception;
        }
    }

    /**
     * Changes the signed-in administrator's password. This route is protected
     * by the normal JWT filter; only login and first-admin setup are public.
     */
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        administratorService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
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

    private String loginKey(String email) {
        return "login:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }
}
