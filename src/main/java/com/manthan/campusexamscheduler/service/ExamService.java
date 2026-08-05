package com.manthan.campusexamscheduler.service;
import com.manthan.campusexamscheduler.dto.ExamRequest;
import com.manthan.campusexamscheduler.dto.ExamResponse;
import com.manthan.campusexamscheduler.dto.StudentRequest;
import com.manthan.campusexamscheduler.dto.StudentResponse;
import com.manthan.campusexamscheduler.entity.Department;
import com.manthan.campusexamscheduler.entity.Exam;
import com.manthan.campusexamscheduler.entity.Student;
import com.manthan.campusexamscheduler.entity.Subject;
import com.manthan.campusexamscheduler.exception.ResourceNotFoundException;
import com.manthan.campusexamscheduler.repository.ExamRepository;
import com.manthan.campusexamscheduler.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;

    public ExamResponse createExam(ExamRequest request) {

        // Check if subject exists
        Subject subject = subjectRepository
                .findById(request.getSubjectId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Subject not found"));

        // DTO -> Entity
        Exam exam = Exam.builder()
                .examDate(request.getExamDate())
                .examTime(request.getExamTime())
                .roomNumber(request.getRoomNumber())
                .building(request.getBuilding())
                .subject(subject)
                .build();

        // Save Exam
        Exam savedExam = examRepository.save(exam);

        // Entity -> Response DTO
        return ExamResponse.builder()
                .examId(savedExam.getExamId())
                .examDate(savedExam.getExamDate())
                .examTime(savedExam.getExamTime())
                .roomNumber(savedExam.getRoomNumber())
                .subjectId(savedExam.getSubject().getSubjectId())
                .subjectCode(savedExam.getSubject().getSubjectCode())
                .subjectName(savedExam.getSubject().getSubjectName())
                .building(savedExam.getBuilding())
                .build();
    }

    // GET ALL
    public List<ExamResponse> getAllExams() {

        List<Exam> exams = examRepository.findAllWithSubject();

        return exams.stream()
                .map(exam -> ExamResponse.builder()
                        .examId(exam.getExamId())
                        .examDate(exam.getExamDate())
                        .examTime(exam.getExamTime())
                        .roomNumber(exam.getRoomNumber())
                        .subjectId(exam.getSubject().getSubjectId())
                        .subjectCode(exam.getSubject().getSubjectCode())
                        .subjectName(exam.getSubject().getSubjectName())
                        .building(exam.getBuilding())
                        .build())
                .toList();
    }

    // GET BY ID
    public ExamResponse getExamById(Long id) {

        Exam exam = examRepository.findByIdWithSubject(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Exam not found"));

        return ExamResponse.builder()
                .subjectName(exam.getSubject().getSubjectName())
                .examId(exam.getExamId())
                .examTime(exam.getExamTime())
                .examDate(exam.getExamDate())
                .roomNumber(exam.getRoomNumber())
                .subjectCode(exam.getSubject().getSubjectCode())
                .building(exam.getBuilding())
                .subjectId(exam.getSubject().getSubjectId())
                .build();
    }

    // UPDATE
    public ExamResponse updateExam(Long id, ExamRequest request) {

        Exam exam = examRepository.findByIdWithSubject(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Exam not found"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Subject not found"));

        exam.setExamDate(request.getExamDate());
        exam.setExamTime(request.getExamTime());
        exam.setRoomNumber(request.getRoomNumber());
        exam.setBuilding(request.getBuilding());
        exam.setSubject(subject);

        Exam updatedExam = examRepository.save(exam);

        return ExamResponse.builder()
                .examId(updatedExam.getExamId())
                .examDate(updatedExam.getExamDate())
                .examTime(updatedExam.getExamTime())
                .roomNumber(updatedExam.getRoomNumber())
                .subjectId(updatedExam.getSubject().getSubjectId())
                .subjectCode(updatedExam.getSubject().getSubjectCode())
                .subjectName(updatedExam.getSubject().getSubjectName())
                .building(updatedExam.getBuilding())
                .build();
    }

    // DELETE
    public void deleteExamById(Long id) {

        Exam exam = examRepository.findByIdWithSubject(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Exam not found"));

        examRepository.delete(exam);
    }
}