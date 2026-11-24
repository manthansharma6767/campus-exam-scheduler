package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.SeatLookupResponse;
import com.manthan.campusexamscheduler.dto.SeatingPlanCsvExport;
import com.manthan.campusexamscheduler.dto.SeatingPlanImportResponse;
import com.manthan.campusexamscheduler.dto.SeatingPlanResponse;
import com.manthan.campusexamscheduler.service.SeatingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/seating/plans")
@RequiredArgsConstructor
public class SeatingPlanController {

    private final SeatingPlanService seatingPlanService;

    @GetMapping
    public ResponseEntity<List<SeatingPlanResponse>> getPlans() {
        return ResponseEntity.ok(seatingPlanService.getPlans());
    }

    @GetMapping("/{planId}/roll/{rollNumber}")
    public ResponseEntity<SeatLookupResponse> findSeat(
            @PathVariable Long planId,
            @PathVariable String rollNumber) {
        return ResponseEntity.ok(seatingPlanService.findSeat(planId, rollNumber));
    }

    @GetMapping(value = "/{planId}/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportPlan(@PathVariable Long planId) {
        SeatingPlanCsvExport export = seatingPlanService.exportPlanCsv(planId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(export.filename()).build().toString())
                .body(export.content());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SeatingPlanImportResponse> importPlan(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title) {
        return ResponseEntity.ok(seatingPlanService.importPdf(file, title));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long planId) {
        seatingPlanService.deletePlan(planId);
        return ResponseEntity.noContent().build();
    }
}
