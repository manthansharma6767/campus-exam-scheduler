package com.manthan.campusexamscheduler.service;
import com.manthan.campusexamscheduler.dto.ExamRequest;
import com.manthan.campusexamscheduler.dto.ExamResponse;
import com.manthan.campusexamscheduler.entity.Exam;
import com.manthan.campusexamscheduler.entity.Subject;
import com.manthan.campusexamscheduler.exception.ResourceNotFoundException;
import com.manthan.campusexamscheduler.exception.DuplicateResourceException;
import com.manthan.campusexamscheduler.exception.ScheduleConflictException;
import com.manthan.campusexamscheduler.repository.ExamRepository;
import com.manthan.campusexamscheduler.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final ScheduleConflictService scheduleConflictService;

    @Transactional(readOnly = true)
    public List<ExamResponse> searchPublicExams(String query, Integer semester, LocalDate examDate) {
        if (semester != null && (semester < 1 || semester > 8)) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return examRepository.searchPublicExams(normalizedQuery, semester, examDate)
                .stream()
                .map(exam -> ExamResponse.builder()
                        .examId(exam.getExamId())
                        .examDate(exam.getExamDate())
                        .examTime(exam.getExamTime())
                        .examEndTime(exam.getExamEndTime())
                        .roomNumber(exam.getRoomNumber())
                        .subjectId(exam.getSubject().getSubjectId())
                        .subjectCode(exam.getSubject().getSubjectCode())
                        .subjectName(exam.getSubject().getSubjectName())
                        .building(exam.getBuilding())
                        .build())
                .toList();
    }

    @Transactional
    public ExamResponse createExam(ExamRequest request) {
        validateExamTimes(request);

        // Check if subject exists
        Subject subject = subjectRepository
                .findById(request.getSubjectId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Subject not found"));

        if (examRepository.existsBySubject_SubjectIdAndExamDateAndExamTime(
                subject.getSubjectId(), request.getExamDate(), request.getExamTime())) {
            throw new DuplicateResourceException("An exam for this subject, date, and time already exists.");
        }

        Exam exam = Exam.builder()
                .examDate(request.getExamDate())
                .examTime(request.getExamTime())
                .examEndTime(request.getExamEndTime())
                .roomNumber(request.getRoomNumber())
                .building(request.getBuilding())
                .subject(subject)
                .build();

        rejectScheduleConflicts(exam, null);

        // Save Exam
        Exam savedExam = examRepository.save(exam);

        // Entity -> Response DTO
        return ExamResponse.builder()
                .examId(savedExam.getExamId())
                .examDate(savedExam.getExamDate())
                .examTime(savedExam.getExamTime())
                .examEndTime(savedExam.getExamEndTime())
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
                        .examEndTime(exam.getExamEndTime())
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
                .examEndTime(exam.getExamEndTime())
                .examDate(exam.getExamDate())
                .roomNumber(exam.getRoomNumber())
                .subjectCode(exam.getSubject().getSubjectCode())
                .building(exam.getBuilding())
                .subjectId(exam.getSubject().getSubjectId())
                .build();
    }

    // UPDATE
    @Transactional
    public ExamResponse updateExam(Long id, ExamRequest request) {
        validateExamTimes(request);

        Exam exam = examRepository.findByIdWithSubject(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Exam not found"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Subject not found"));

        if (examRepository.existsBySubject_SubjectIdAndExamDateAndExamTimeAndExamIdNot(
                subject.getSubjectId(), request.getExamDate(), request.getExamTime(), id)) {
            throw new DuplicateResourceException("An exam for this subject, date, and time already exists.");
        }

        Exam candidate = Exam.builder()
                .examId(id)
                .examDate(request.getExamDate())
                .examTime(request.getExamTime())
                .examEndTime(request.getExamEndTime())
                .roomNumber(request.getRoomNumber())
                .building(request.getBuilding())
                .subject(subject)
                .build();
        rejectScheduleConflicts(candidate, id);

        exam.setExamDate(request.getExamDate());
        exam.setExamTime(request.getExamTime());
        exam.setExamEndTime(request.getExamEndTime());
        exam.setRoomNumber(request.getRoomNumber());
        exam.setBuilding(request.getBuilding());
        exam.setSubject(subject);

        Exam updatedExam = examRepository.save(exam);

        return ExamResponse.builder()
                .examId(updatedExam.getExamId())
                .examDate(updatedExam.getExamDate())
                .examTime(updatedExam.getExamTime())
                .examEndTime(updatedExam.getExamEndTime())
                .roomNumber(updatedExam.getRoomNumber())
                .subjectId(updatedExam.getSubject().getSubjectId())
                .subjectCode(updatedExam.getSubject().getSubjectCode())
                .subjectName(updatedExam.getSubject().getSubjectName())
                .building(updatedExam.getBuilding())
                .build();
    }

    private void rejectScheduleConflicts(Exam candidate, Long ignoredExamId) {
        var conflicts = scheduleConflictService.findConflicts(
                candidate, examRepository.findAllWithSubject(), 0, ignoredExamId);
        if (!conflicts.isEmpty()) {
            throw new ScheduleConflictException(conflicts.stream()
                    .map(conflict -> conflict.message())
                    .distinct()
                    .reduce((left, right) -> left + " " + right)
                    .orElse("The exam conflicts with the current schedule."));
        }
    }

    private void validateExamTimes(ExamRequest request) {
        if (request.getExamTime() != null && request.getExamEndTime() != null
                && !request.getExamEndTime().isAfter(request.getExamTime())) {
            throw new IllegalArgumentException("Exam end time must be later than its start time.");
        }
    }

    // DELETE
    public void deleteExamById(Long id) {

        Exam exam = examRepository.findByIdWithSubject(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Exam not found"));

        examRepository.delete(exam);
    }
}
