package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.StudentRequest;
import com.manthan.campusexamscheduler.dto.StudentResponse;
import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Student;
import com.manthan.campusexamscheduler.repository.DepartmentRepository;
import com.manthan.campusexamscheduler.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    private final DepartmentRepository departmentRepository;

    public StudentResponse registerStudent(StudentRequest request) {
        if(studentRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email Not Exists");
        }
        if(studentRepository.existsByEnrollmentNumber(request.getEnrollmentNumber())) {
            throw new RuntimeException("Not Exists");
        }
        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new RuntimeException("Department not found"));

        Student student = Student.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .enrollmentNumber(request.getEnrollmentNumber())
                .semester(request.getSemester())
                .department(department)
                .build();

        Student savedStudent = studentRepository.save(student);

        StudentResponse studentResponse = StudentResponse.builder()
                .studentId(savedStudent.getStudentId())
                .name(savedStudent.getName())
                .email(savedStudent.getEmail())
                .enrollmentNumber(savedStudent.getEnrollmentNumber())
                .semester(savedStudent.getSemester())
                .departmentId(savedStudent.getDepartment().getDepartmentId())
                .departmentName(savedStudent.getDepartment().getDepartmentName())
                .build();
        return studentResponse;
    }
}
