package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.StudentAbsenteeExamOptionResponse;
import com.manthan.campusexamscheduler.dto.StudentRequest;
import com.manthan.campusexamscheduler.dto.StudentResponse;
import com.manthan.campusexamscheduler.dto.StudentScheduleResponse;
import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Exam;
import com.manthan.campusexamscheduler.entity.ExamType;
import com.manthan.campusexamscheduler.entity.Student;
import com.manthan.campusexamscheduler.entity.StudentAbsenteeExam;
import com.manthan.campusexamscheduler.exception.ResourceNotFoundException;
import com.manthan.campusexamscheduler.exception.DuplicateResourceException;
import com.manthan.campusexamscheduler.repository.DepartmentRepository;
import com.manthan.campusexamscheduler.repository.ExamRepository;
import com.manthan.campusexamscheduler.repository.StudentAbsenteeExamRepository;
import com.manthan.campusexamscheduler.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final ExamRepository examRepository;
    private final StudentAbsenteeExamRepository studentAbsenteeExamRepository;
    private final PasswordEncoder passwordEncoder;

    // CREATE
    public StudentResponse registerStudent(StudentRequest request) {

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required when creating a student.");
        }

        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A student with this email already exists.");
        }

        if (studentRepository.existsByEnrollmentNumber(request.getEnrollmentNumber())) {
            throw new DuplicateResourceException("Enrollment number already exists.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found"));

        Student student = Student.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student not found"));

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
    @Transactional
    public StudentResponse updateStudent(Long id, StudentRequest request) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student not found"));

        if (studentRepository.existsByEmailAndStudentIdNot(request.getEmail(), id)) {
            throw new DuplicateResourceException("A student with this email already exists.");
        }
        if (studentRepository.existsByEnrollmentNumberAndStudentIdNot(request.getEnrollmentNumber(), id)) {
            throw new DuplicateResourceException("Enrollment number already exists.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found"));

        student.setName(request.getName());
        student.setEmail(request.getEmail());

        // Keep the current password when the edit form leaves the field blank.
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            student.setPassword(passwordEncoder.encode(request.getPassword()));
        }

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
    @Transactional
    public void deleteStudent(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student not found"));

        studentAbsenteeExamRepository.deleteByStudent_StudentId(id);
        studentAbsenteeExamRepository.flush();
        studentRepository.delete(student);
    }

    // Absentee eligibility (admin)
    @Transactional(readOnly = true)
    public List<StudentAbsenteeExamOptionResponse> getAbsenteeExamOptions(Long studentId) {

        Student student = findStudent(studentId);
        List<Exam> exams = examRepository
                .findBySubject_DepartmentAndSubject_SemesterAndExamTypeOrderByExamDateAscExamTimeAsc(
                        student.getDepartment(),
                        student.getSemester(),
                        ExamType.ABSENTEE
                );
        Set<Long> assignedExamIds = new HashSet<>(
                studentAbsenteeExamRepository.findExamIdsByStudentId(studentId)
        );

        return exams.stream()
                .map(exam -> toAbsenteeExamOption(exam, assignedExamIds.contains(exam.getExamId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentAbsenteeExamOptionResponse> getAbsenteeExamOptionsForEnrollment(
            String enrollmentNumber) {

        return getAbsenteeExamOptions(findStudentByEnrollment(enrollmentNumber).getStudentId());
    }

    @Transactional
    public List<StudentAbsenteeExamOptionResponse> updateAbsenteeEligibilityForEnrollment(
            String enrollmentNumber,
            List<Long> examIds) {

        return updateAbsenteeEligibility(
                findStudentByEnrollment(enrollmentNumber).getStudentId(),
                examIds
        );
    }

    @Transactional
    public List<StudentAbsenteeExamOptionResponse> updateAbsenteeEligibility(
            Long studentId,
            List<Long> examIds) {

        Student student = findStudent(studentId);
        List<Long> requestedExamIds = examIds == null
                ? List.of()
                : examIds.stream().filter(Objects::nonNull).distinct().toList();

        List<Exam> requestedExams = examRepository.findAllById(requestedExamIds);
        if (requestedExams.size() != requestedExamIds.size()) {
            throw new ResourceNotFoundException("One or more absentee exams were not found.");
        }

        for (Exam exam : requestedExams) {
            validateAbsenteeExamForStudent(exam, student);
        }

        Map<Long, StudentAbsenteeExam> existingByExamId =
                studentAbsenteeExamRepository.findByStudent_StudentId(studentId)
                        .stream()
                        .collect(Collectors.toMap(
                                mapping -> mapping.getExam().getExamId(),
                                Function.identity()
                        ));

        List<StudentAbsenteeExam> mappingsToRemove = existingByExamId.entrySet().stream()
                .filter(entry -> !requestedExamIds.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        studentAbsenteeExamRepository.deleteAll(mappingsToRemove);

        for (Exam exam : requestedExams) {
            if (!existingByExamId.containsKey(exam.getExamId())) {
                studentAbsenteeExamRepository.save(StudentAbsenteeExam.builder()
                        .student(student)
                        .exam(exam)
                        .build());
            }
        }

        return getAbsenteeExamOptions(studentId);
    }

    // Student Schedule
    @Transactional(readOnly = true)
    public List<StudentScheduleResponse> getStudentSchedule(
            String enrollmentNumber) {

        Student student = findStudentByEnrollment(enrollmentNumber);

        List<Exam> absenteeExams = studentAbsenteeExamRepository.findEligibleAbsenteeExams(
                student.getStudentId(),
                student.getDepartment().getDepartmentId(),
                student.getSemester(),
                ExamType.ABSENTEE
        );
        Set<Long> absenteeSubjectIds = absenteeExams.stream()
                .map(exam -> exam.getSubject().getSubjectId())
                .collect(Collectors.toSet());

        List<Exam> exams = new ArrayList<>(examRepository
                .findBySubject_DepartmentAndSubject_SemesterAndExamTypeOrderByExamDateAscExamTimeAsc(
                        student.getDepartment(),
                        student.getSemester(),
                        ExamType.REGULAR
                )
                .stream()
                .filter(exam -> !absenteeSubjectIds.contains(exam.getSubject().getSubjectId()))
                .toList());
        exams.addAll(absenteeExams);
        exams.sort(Comparator
                .comparing(Exam::getExamDate)
                .thenComparing(Exam::getExamTime)
                .thenComparing(exam -> exam.getSubject().getSubjectCode()));

        return exams.stream()
                .map(exam -> StudentScheduleResponse.builder()
                        .subjectCode(exam.getSubject().getSubjectCode())
                        .subjectName(exam.getSubject().getSubjectName())
                        .examDate(exam.getExamDate())
                        .examTime(exam.getExamTime())
                        .examEndTime(exam.getExamEndTime())
                        .building(exam.getBuilding())
                        .roomNumber(exam.getRoomNumber())
                        .examType(exam.getExamType())
                        .build())
                .toList();
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private Student findStudentByEnrollment(String enrollmentNumber) {
        return studentRepository.findByEnrollmentNumber(enrollmentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private void validateAbsenteeExamForStudent(Exam exam, Student student) {
        if (exam.getExamType() != ExamType.ABSENTEE) {
            throw new IllegalArgumentException("Only absentee exams can be assigned to a student.");
        }
        if (!exam.getSubject().getDepartment().getDepartmentId()
                .equals(student.getDepartment().getDepartmentId())
                || !exam.getSubject().getSemester().equals(student.getSemester())) {
            throw new IllegalArgumentException(
                    "Absentee exams must belong to the student's department and semester.");
        }
    }

    private StudentAbsenteeExamOptionResponse toAbsenteeExamOption(Exam exam, boolean assigned) {
        return StudentAbsenteeExamOptionResponse.builder()
                .examId(exam.getExamId())
                .subjectCode(exam.getSubject().getSubjectCode())
                .subjectName(exam.getSubject().getSubjectName())
                .examDate(exam.getExamDate())
                .examTime(exam.getExamTime())
                .examEndTime(exam.getExamEndTime())
                .building(exam.getBuilding())
                .roomNumber(exam.getRoomNumber())
                .examType(exam.getExamType())
                .assigned(assigned)
                .build();
    }
}
