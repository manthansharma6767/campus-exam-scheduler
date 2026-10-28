package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.TimetableImportResponse;
import com.manthan.campusexamscheduler.service.TimetableImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class TimetableImportController {

    private final TimetableImportService timetableImportService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TimetableImportResponse> importTimetable(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(timetableImportService.importTimetable(file));
    }
}
