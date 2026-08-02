package com.manthan.campusexamscheduler.service;


import com.manthan.campusexamscheduler.dto.DepartmentRequest;
import com.manthan.campusexamscheduler.dto.DepartmentResponse;
import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.exception.ResourceNotFoundException;
import com.manthan.campusexamscheduler.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentResponse createDepartment(DepartmentRequest request) throws Exception {
        if(departmentRepository.existsByDepartmentName(request.getDepartmentName())) {
            throw new ResourceNotFoundException("Department already exists");
        }
        Department department = Department.builder()
                .departmentName(request.getDepartmentName())
                .build();
        Department savedDepartment = departmentRepository.save(department);
        DepartmentResponse response = DepartmentResponse.builder()
                .departmentId(savedDepartment.getDepartmentId())
                .departmentName(savedDepartment.getDepartmentName())
                .build();
        return response;
    }

    public List<DepartmentResponse> getAllDepartments() {

        List<Department> departments = departmentRepository.findAll();

        return departments.stream()
                .map(department -> DepartmentResponse
                        .builder()
                        .departmentId(department.getDepartmentId())
                        .departmentName(department.getDepartmentName())
                        .build())
                .toList();
    }


    public DepartmentResponse findById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        return DepartmentResponse
                .builder()
                .departmentId(department.getDepartmentId())
                .departmentName(department.getDepartmentName())
                .build();
    }

    public DepartmentResponse updateDepartment(
            Long id,
            DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        department.setDepartmentName(request.getDepartmentName());

        Department updatedDepartment = departmentRepository.save(department);

        return DepartmentResponse.builder()
                .departmentId(updatedDepartment.getDepartmentId())
                .departmentName(updatedDepartment.getDepartmentName())
                .build();
    }

    public void deleteDepartment(Long id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        departmentRepository.delete(department);
    }
}
