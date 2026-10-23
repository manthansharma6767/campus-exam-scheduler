package com.manthan.campusexamscheduler.controller;

import com.manthan.campusexamscheduler.dto.SeatLookupResponse;
import com.manthan.campusexamscheduler.dto.SeatingPlanImportResponse;
import com.manthan.campusexamscheduler.dto.SeatingPlanResponse;
import com.manthan.campusexamscheduler.service.SeatingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
