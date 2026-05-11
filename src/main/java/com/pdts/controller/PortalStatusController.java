// Path: src/main/java/com/pdts/controller/PortalStatusController.java
package com.pdts.controller;

import com.pdts.service.TokenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal")
public class PortalStatusController {

    @Autowired
    private TokenService tokenService;

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam String ref,
            @RequestParam String token) {
        try {
            tokenService.verifyToken(ref.trim(), token.trim());

            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT application_reference_number, applicant_full_name, program_name, " +
                            "campus_name, application_semester, application_academic_year, " +
                            "application_status_name, requirement_tracking_no, requirement_type_name, " +
                            "requirement_status_name, requirement_status_color, rejection_reason_description, " +
                            "requirement_upload_date, requirement_processed_at, resubmission_notes " +
                            "FROM vw_student_status " +
                            "WHERE application_reference_number = :ref")
                    .setParameter("ref", ref.trim())
                    .getResultList();

            List<Map<String, Object>> result = rows.stream().map(row -> Map.ofEntries(
                    Map.entry("applicationReferenceNumber", row[0] != null ? row[0] : ""),
                    Map.entry("applicantFullName", row[1] != null ? row[1] : ""),
                    Map.entry("programName", row[2] != null ? row[2] : ""),
                    Map.entry("campusName", row[3] != null ? row[3] : ""),
                    Map.entry("applicationSemester", row[4] != null ? row[4] : ""),
                    Map.entry("applicationAcademicYear", row[5] != null ? row[5] : ""),
                    Map.entry("applicationStatusName", row[6] != null ? row[6] : ""),
                    Map.entry("requirementTrackingNo", row[7] != null ? row[7] : ""),
                    Map.entry("requirementTypeName", row[8] != null ? row[8] : ""),
                    Map.entry("requirementStatusName", row[9] != null ? row[9] : ""),
                    Map.entry("requirementStatusColor", row[10] != null ? row[10] : "#888"),
                    Map.entry("rejectionReasonDescription", row[11] != null ? row[11] : ""),
                    Map.entry("requirementUploadDate", row[12] != null ? row[12].toString() : ""),
                    Map.entry("requirementProcessedAt", row[13] != null ? row[13].toString() : ""),
                    Map.entry("resubmissionNotes", row[14] != null ? row[14] : ""))).toList();

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid or expired token. Please contact the OUS Registrar's Office."));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyGet(@RequestParam String referenceNo,
            @RequestParam String token) {
        try {
            tokenService.verifyToken(referenceNo.trim(), token.trim());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
