package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.StudentRequest;
import com.manthan.campusexamscheduler.dto.StudentResponse;
import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Student;
import com.manthan.campusexamscheduler.repository.DepartmentRepository;
import com.manthan.campusexamscheduler.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;

    // CREATE
    public StudentResponse registerStudent(StudentRequest request) {

        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (studentRepository.existsByEnrollmentNumber(request.getEnrollmentNumber())) {
            throw new RuntimeException("Enrollment number already exists");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
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

        return StudentResponse.builder()
                .studentId(savedStudent.getStudentId())
                .name(savedStudent.getName())
                .email(savedStudent.getEmail())
                .enrollmentNumber(savedStudent.getEnrollmentNumber())
                .semester(savedStudent.getSemester())
                .departmentId(savedStudent.getDepartment().getDepartmentId())
                .departmentName(savedStudent.getDepartment().getDepartmentName())
                .build();
    }

    // GET ALL
    public List<StudentResponse> getAllStudents() {

        List<Student> students = studentRepository.findAll();

        return students.stream()
                .map(student -> StudentResponse.builder()
                        .studentId(student.getStudentId())
                        .name(student.getName())
                        .email(student.getEmail())
                        .enrollmentNumber(student.getEnrollmentNumber())
                        .semester(student.getSemester())
                        .departmentId(student.getDepartment().getDepartmentId())
                        .departmentName(student.getDepartment().getDepartmentName())
                        .build())
                .toList();
    }

    // GET BY ID
    public StudentResponse getStudentById(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        return StudentResponse.builder()
                .studentId(student.getStudentId())
                .name(student.getName())
                .email(student.getEmail())
                .enrollmentNumber(student.getEnrollmentNumber())
                .semester(student.getSemester())
                .departmentId(student.getDepartment().getDepartmentId())
                .departmentName(student.getDepartment().getDepartmentName())
                .build();
    }

    // UPDATE
    public StudentResponse updateStudent(Long id, StudentRequest request) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new RuntimeException("Department not found"));

        student.setName(request.getName());
        student.setEmail(request.getEmail());
        student.setPassword(request.getPassword());
        student.setEnrollmentNumber(request.getEnrollmentNumber());
        student.setSemester(request.getSemester());
        student.setDepartment(department);

        Student updatedStudent = studentRepository.save(student);

        return StudentResponse.builder()
                .studentId(updatedStudent.getStudentId())
                .name(updatedStudent.getName())
                .email(updatedStudent.getEmail())
                .enrollmentNumber(updatedStudent.getEnrollmentNumber())
                .semester(updatedStudent.getSemester())
                .departmentId(updatedStudent.getDepartment().getDepartmentId())
                .departmentName(updatedStudent.getDepartment().getDepartmentName())
                .build();
    }

    // DELETE
    public void deleteStudent(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        studentRepository.delete(student);
    }
}