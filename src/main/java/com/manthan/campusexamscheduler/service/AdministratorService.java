package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.*;
import com.manthan.campusexamscheduler.entity.Administrator;
import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Student;
import com.manthan.campusexamscheduler.repository.AdministratorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AdministratorService {

    private final AdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;

    public AdministratorResponse registerAdmin(AdministratorRequest request) {
        if(administratorRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email Exists");
        }

        Administrator administrator = Administrator.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        Administrator savedAdmin = administratorRepository.save(administrator);

        AdministratorResponse adminResponse = AdministratorResponse.builder()
                .adminId(savedAdmin.getAdminId())
                .name(savedAdmin.getName())
                .email(savedAdmin.getEmail())
                .build();
        return adminResponse;
    }

    public List<AdministratorResponse> getAllAdmins() {

        List<Administrator> administrators = administratorRepository.findAll();

        return administrators.stream()
                .map(administrator -> AdministratorResponse .builder()
                        .adminId(administrator.getAdminId())
                        .email(administrator.getEmail())
                        .name(administrator.getName())
                        .build()).toList();

    }

    public AdministratorResponse getAdminById(Long id) {

        Administrator administrator = administratorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Administrator not found"));

        return AdministratorResponse.builder()
                .adminId(administrator.getAdminId())
                .name(administrator.getName())
                .email(administrator.getEmail())
                .build();
    }

    public AdministratorResponse updateAdmin(
            Long id,
            AdministratorRequest request) {
        Administrator administrator = administratorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        administrator.setName(request.getName());
        administrator.setEmail(request.getEmail());
        administrator.setPassword(request.getPassword());

        Administrator updatedAdmin = administratorRepository.save(administrator);

        return AdministratorResponse.builder()
                .adminId(updatedAdmin.getAdminId())
                .email(updatedAdmin.getEmail())
                .name(updatedAdmin.getName())
                .build();
    }

    public void deleteAdmin(Long id) {

        Administrator admin = administratorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        administratorRepository.delete(admin);
    }
}
