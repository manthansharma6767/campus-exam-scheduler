package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.SubjectRequest;
import com.manthan.campusexamscheduler.dto.SubjectResponse;
import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Subject;
import com.manthan.campusexamscheduler.exception.ResourceNotFoundException;
import com.manthan.campusexamscheduler.repository.DepartmentRepository;
import com.manthan.campusexamscheduler.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;

    public SubjectResponse createSubject(SubjectRequest request) {

        if (subjectRepository.existsBySubjectCode(request.getSubjectCode())) {
            throw new ResourceNotFoundException("Subject code already exists");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Subject subject = Subject.builder()
                .subjectCode(request.getSubjectCode())
                .subjectName(request.getSubjectName())
                .semester(request.getSemester())
                .department(department)
                .build();

        Subject savedSubject = subjectRepository.save(subject);

        return SubjectResponse.builder()
                .subjectId(savedSubject.getSubjectId())
                .subjectCode(savedSubject.getSubjectCode())
                .subjectName(savedSubject.getSubjectName())
                .semester(savedSubject.getSemester())
                .departmentId(savedSubject.getDepartment().getDepartmentId())
                .departmentName(savedSubject.getDepartment().getDepartmentName())
                .build();
    }

    public List<SubjectResponse> getAllSubjects() {

        List<Subject> subjects = subjectRepository.findAll();

        return subjects.stream()
                .map(subject -> SubjectResponse.builder()
                        .subjectId(subject.getSubjectId())
                        .subjectCode(subject.getSubjectCode())
                        .subjectName(subject.getSubjectName())
                        .semester(subject.getSemester())
                        .departmentId(subject.getDepartment().getDepartmentId())
                        .departmentName(subject.getDepartment().getDepartmentName())
                        .build())
                .toList();
    }

    public SubjectResponse getSubjectById(Long id) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        return SubjectResponse.builder()
                .subjectId(subject.getSubjectId())
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .semester(subject.getSemester())
                .departmentId(subject.getDepartment().getDepartmentId())
                .departmentName(subject.getDepartment().getDepartmentName())
                .build();
    }

    public SubjectResponse updateSubject(Long id, SubjectRequest request) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setSemester(request.getSemester());
        subject.setDepartment(department);

        Subject updatedSubject = subjectRepository.save(subject);

        return SubjectResponse.builder()
                .subjectId(updatedSubject.getSubjectId())
                .subjectCode(updatedSubject.getSubjectCode())
                .subjectName(updatedSubject.getSubjectName())
                .semester(updatedSubject.getSemester())
                .departmentId(updatedSubject.getDepartment().getDepartmentId())
                .departmentName(updatedSubject.getDepartment().getDepartmentName())
                .build();
    }

    public void deleteSubject(Long id) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        subjectRepository.delete(subject);
    }
}