package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.AuthenticationRequest;
import com.manthan.campusexamscheduler.dto.AuthenticationResponse;
import com.manthan.campusexamscheduler.dto.AdministratorRequest;
import com.manthan.campusexamscheduler.dto.AdministratorResponse;
import com.manthan.campusexamscheduler.entity.Administrator;
import com.manthan.campusexamscheduler.exception.DuplicateResourceException;
import com.manthan.campusexamscheduler.repository.AdministratorRepository;
import com.manthan.campusexamscheduler.util.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_SETUP_TOKEN:}")
    private String adminSetupToken;

    public AuthenticationResponse login(AuthenticationRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String jwtToken = jwtService.generateToken(request.getEmail());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    /**
     * Bootstrap endpoint: creates the very first administrator account.
     * Throws {@link IllegalStateException} if any admin already exists, so
     * this endpoint is a one-time setup convenience and cannot be abused to
     * create additional admins once the system is in use.
     */
    @Transactional
    public synchronized AdministratorResponse bootstrapAdmin(AdministratorRequest request, String setupToken) {
        if (adminSetupToken == null || adminSetupToken.isBlank()) {
            throw new IllegalArgumentException("First-admin setup is disabled. Configure ADMIN_SETUP_TOKEN on the backend first.");
        }
        if (setupToken == null || !MessageDigest.isEqual(
                adminSetupToken.getBytes(StandardCharsets.UTF_8),
                setupToken.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("The setup token is invalid.");
        }
        if (administratorRepository.count() > 0) {
            throw new IllegalArgumentException(
                    "An administrator account already exists. Use the admin panel to manage accounts.");
        }
        if (administratorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An administrator with this email already exists.");
        }
        Administrator admin = Administrator.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        Administrator saved = administratorRepository.save(admin);
        return AdministratorResponse.builder()
                .adminId(saved.getAdminId())
                .name(saved.getName())
                .email(saved.getEmail())
                .build();
    }
}
