package com.manthan.campusexamscheduler.service;


import com.manthan.campusexamscheduler.dto.DepartmentRequest;
import com.manthan.campusexamscheduler.dto.DepartmentResponse;
import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentResponse createDepartment(DepartmentRequest request) throws Exception {
        if(departmentRepository.existsByDepartmentName(request.getDepartmentName())) {
            throw new RuntimeException("Department already exists");
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

}
