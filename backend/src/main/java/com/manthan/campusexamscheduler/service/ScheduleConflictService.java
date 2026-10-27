package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.dto.ConflictDetail;
import com.manthan.campusexamscheduler.entity.Exam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ScheduleConflictService {

    public List<ConflictDetail> findConflicts(
            Exam candidate,
            Collection<Exam> scheduledExams,
            int rowNumber,
            Long ignoredExamId) {
        List<ConflictDetail> conflicts = new ArrayList<>();
        for (Exam scheduled : scheduledExams) {
            if (scheduled == null
                    || (ignoredExamId != null && Objects.equals(ignoredExamId, scheduled.getExamId()))
                    || !Objects.equals(candidate.getExamDate(), scheduled.getExamDate())
                    || !overlaps(candidate, scheduled)) {
                continue;
            }

            if (sameAssignedRoom(candidate, scheduled)) {
                conflicts.add(new ConflictDetail(rowNumber, "ROOM",
                        "Room " + candidate.getBuilding() + " / " + candidate.getRoomNumber()
                                + " is already booked at this date and time for "
                                + scheduled.getSubject().getSubjectCode() + "."));
            }

            var candidateSubject = candidate.getSubject();
            var scheduledSubject = scheduled.getSubject();
            if (!Objects.equals(candidateSubject.getSubjectId(), scheduledSubject.getSubjectId())
                    && candidateSubject.getSemester() != null
                    && scheduledSubject.getSemester() != null
                    && Objects.equals(candidateSubject.getSemester(), scheduledSubject.getSemester())
                    && candidateSubject.getDepartment() != null
                    && scheduledSubject.getDepartment() != null
                    && Objects.equals(candidateSubject.getDepartment().getDepartmentId(),
                            scheduledSubject.getDepartment().getDepartmentId())) {
                conflicts.add(new ConflictDetail(rowNumber, "STUDENT",
                        "Students in " + candidateSubject.getDepartment().getDepartmentName()
                                + " semester " + candidateSubject.getSemester()
                                + " already have " + scheduledSubject.getSubjectCode()
                                + " at this date and time."));
            }
        }
        return conflicts;
    }

    private boolean overlaps(Exam left, Exam right) {
        if (left.getExamTime() == null || right.getExamTime() == null) return false;

        // Old records and sources with a single time have no known duration. Preserve the
        // original same-start protection without guessing how long those exams last.
        if (left.getExamEndTime() == null || right.getExamEndTime() == null) {
            return left.getExamTime().equals(right.getExamTime());
        }

        return left.getExamTime().isBefore(right.getExamEndTime())
                && right.getExamTime().isBefore(left.getExamEndTime());
    }

    private boolean sameAssignedRoom(Exam left, Exam right) {
        String leftBuilding = normalizeLocation(left.getBuilding());
        String rightBuilding = normalizeLocation(right.getBuilding());
        String leftRoom = normalizeLocation(left.getRoomNumber());
        String rightRoom = normalizeLocation(right.getRoomNumber());
        return !leftBuilding.isBlank() && !rightBuilding.isBlank()
                && !leftRoom.isBlank() && !rightRoom.isBlank()
                && !leftBuilding.equals("not assigned") && !leftRoom.equals("not assigned")
                && !rightBuilding.equals("not assigned") && !rightRoom.equals("not assigned")
                && leftBuilding.equals(rightBuilding) && leftRoom.equals(rightRoom);
    }

    private String normalizeLocation(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
