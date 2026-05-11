package com.pdts.controller;

import com.pdts.config.PdtsProperties;
import com.pdts.service.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class RequirementPageController {

    private final JdbcTemplate jdbc;
    private final PdtsProperties pdtsProperties;
    private final AuditLogService auditLogService;

    public RequirementPageController(JdbcTemplate jdbc,
                                     PdtsProperties pdtsProperties,
                                     AuditLogService auditLogService) {
        this.jdbc = jdbc;
        this.pdtsProperties = pdtsProperties;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/requirements")
    public String list(@RequestParam(required = false) Integer statusId, Model model) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    r.requirement_id,
                    r.requirement_tracking_no,
                    r.requirement_file_name,
                    r.requirement_upload_date,
                    r.requirement_status_id,
                    rt.requirement_type_name,
                    rs.requirement_status_name,
                    a.application_reference_number,
                    ap.applicant_id,
                    ap.applicant_first_name,
                    ap.applicant_last_name,
                    ap.applicant_email_address
                FROM requirement r
                JOIN requirement_type rt
                    ON rt.type_id = r.requirement_type_id
                JOIN requirement_status rs
                    ON rs.status_id = r.requirement_status_id
                JOIN application a
                    ON a.application_id = r.application_id
                JOIN applicant ap
                    ON ap.applicant_id = a.applicant_id
                WHERE COALESCE(ap.applicant_is_deleted, 0) = 0
                """);

        List<Object> params = new ArrayList<>();

        if (statusId != null) {
            sql.append(" AND r.requirement_status_id = ? ");
            params.add(statusId);
        }

        sql.append(" ORDER BY r.requirement_upload_date DESC, r.requirement_id DESC");

        model.addAttribute("requirements", jdbc.queryForList(sql.toString(), params.toArray()));
        model.addAttribute("statusId", statusId);
        addLookups(model);

        return "requirements";
    }

    @GetMapping({"/requirements/new", "/requirements/upload"})
    public String uploadForm(Model model) {
        addLookups(model);
        return "requirement-form";
    }

    @PostMapping("/requirements")
    public String create(@RequestParam Integer applicationId,
                         @RequestParam Integer requirementTypeId,
                         @RequestParam MultipartFile file,
                         RedirectAttributes ra) {

        Path savedFilePath = null;

        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Please choose a file to upload.");
            }

            Integer activeApplicationCount = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM application a
                    JOIN applicant ap
                        ON ap.applicant_id = a.applicant_id
                    WHERE a.application_id = ?
                      AND COALESCE(ap.applicant_is_deleted, 0) = 0
                    """, Integer.class, applicationId);

            if (activeApplicationCount == null || activeApplicationCount == 0) {
                throw new IllegalArgumentException("Selected application does not belong to an active applicant.");
            }

            String originalName = file.getOriginalFilename() == null
                    ? "document.pdf"
                    : file.getOriginalFilename();

            String ext = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : ".pdf";

            String savedName = UUID.randomUUID() + ext;

            Path uploadDir = Paths.get(pdtsProperties.getUploadDir());
            Files.createDirectories(uploadDir);

            savedFilePath = uploadDir.resolve(savedName);
            Files.copy(file.getInputStream(), savedFilePath, StandardCopyOption.REPLACE_EXISTING);

            String trackingNo = nextTrackingNo();

            jdbc.update("""
                    INSERT INTO requirement (
                        application_id,
                        requirement_type_id,
                        requirement_status_id,
                        requirement_tracking_no,
                        requirement_file_name,
                        requirement_image_path,
                        requirement_uploaded_by_user_id
                    )
                    VALUES (?, ?, 1, ?, ?, ?, 1)
                    """,
                    applicationId,
                    requirementTypeId,
                    trackingNo,
                    originalName,
                    savedFilePath.toString()
            );

            Integer requirementId = jdbc.queryForObject("""
                    SELECT requirement_id
                    FROM requirement
                    WHERE requirement_tracking_no = ?
                    ORDER BY requirement_id DESC
                    LIMIT 1
                    """, Integer.class, trackingNo);

            auditLogService.log(
                    "UPLOAD_DOCUMENT",
                    "requirement",
                    requirementId != null ? requirementId.longValue() : null,
                    "Uploaded " + originalName + " with tracking number " + trackingNo,
                    null,
                    "Status: Pending"
            );

            ra.addFlashAttribute("success", "Document uploaded. Tracking number: " + trackingNo);
            return "redirect:/requirements";

        } catch (Exception e) {
            if (savedFilePath != null) {
                try {
                    Files.deleteIfExists(savedFilePath);
                } catch (Exception ignored) {
                    // Ignore cleanup error.
                }
            }

            ra.addFlashAttribute("error", "Upload failed: " + e.getMessage());
            return "redirect:/requirements/new";
        }
    }

    @PostMapping("/requirements/{id}/status")
    public String updateStatus(@PathVariable Integer id,
                               @RequestParam Integer statusId,
                               @RequestParam(required = false) Integer rejectionReasonId,
                               @RequestParam(required = false) String remarks,
                               RedirectAttributes ra) {

        try {
            if (!requirementBelongsToActiveApplicant(id)) {
                ra.addFlashAttribute("error", "Status cannot be updated because this requirement belongs to a deleted applicant.");
                return "redirect:/requirements";
            }

            Map<String, Object> requirementInfo = jdbc.queryForMap("""
                    SELECT
                        r.requirement_tracking_no,
                        rt.requirement_type_name,
                        rs.requirement_status_name AS old_status_name
                    FROM requirement r
                    JOIN requirement_type rt
                        ON rt.type_id = r.requirement_type_id
                    JOIN requirement_status rs
                        ON rs.status_id = r.requirement_status_id
                    WHERE r.requirement_id = ?
                    """, id);

            String oldStatus = String.valueOf(requirementInfo.get("old_status_name"));
            String trackingNo = String.valueOf(requirementInfo.get("requirement_tracking_no"));
            String documentType = String.valueOf(requirementInfo.get("requirement_type_name"));

            if (statusId == 3) {
                jdbc.update("""
                        UPDATE requirement
                        SET requirement_status_id = 3,
                            requirement_date_received = CURRENT_TIMESTAMP,
                            requirement_processed_by_user_id = 1,
                            requirement_processed_at = CURRENT_TIMESTAMP
                        WHERE requirement_id = ?
                        """, id);

            } else if (statusId == 4) {
                jdbc.update("""
                        UPDATE requirement
                        SET requirement_status_id = 4,
                            rejection_reason_id = ?,
                            rejection_reason_rejected_by_user_id = 1,
                            rejection_reason_rejected_at = CURRENT_TIMESTAMP,
                            requirement_processed_by_user_id = 1,
                            requirement_processed_at = CURRENT_TIMESTAMP
                        WHERE requirement_id = ?
                        """, rejectionReasonId, id);

            } else if (statusId == 5) {
                jdbc.update("""
                        UPDATE requirement
                        SET requirement_status_id = 5,
                            requirement_remarks = ?,
                            requirement_processed_by_user_id = 1,
                            requirement_processed_at = CURRENT_TIMESTAMP
                        WHERE requirement_id = ?
                        """, remarks, id);

            } else {
                jdbc.update("""
                        UPDATE requirement
                        SET requirement_status_id = ?,
                            requirement_processed_by_user_id = 1,
                            requirement_processed_at = CURRENT_TIMESTAMP
                        WHERE requirement_id = ?
                        """, statusId, id);
            }

            String newStatus = jdbc.queryForObject("""
                    SELECT requirement_status_name
                    FROM requirement_status
                    WHERE status_id = ?
                    """, String.class, statusId);

            auditLogService.log(
                    getRequirementActionType(statusId),
                    "requirement",
                    id.longValue(),
                    "Updated " + documentType + " [" + trackingNo + "] from " + oldStatus + " to " + newStatus,
                    oldStatus,
                    newStatus
            );

            ra.addFlashAttribute("success", "Requirement status updated.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Status update failed: " + e.getMessage());
        }

        return "redirect:/requirements";
    }

    private String getRequirementActionType(Integer statusId) {
        if (statusId == 3) {
            return "RECEIVE_DOCUMENT";
        }

        if (statusId == 4) {
            return "REJECT_DOCUMENT";
        }

        if (statusId == 5) {
            return "FOR_RESUBMISSION";
        }

        if (statusId == 2) {
            return "UNDER_REVIEW";
        }

        return "UPDATE_DOCUMENT_STATUS";
    }

    private boolean requirementBelongsToActiveApplicant(Integer requirementId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM requirement r
                JOIN application a
                    ON a.application_id = r.application_id
                JOIN applicant ap
                    ON ap.applicant_id = a.applicant_id
                WHERE r.requirement_id = ?
                  AND COALESCE(ap.applicant_is_deleted, 0) = 0
                """, Integer.class, requirementId);

        return count != null && count > 0;
    }

    private void addLookups(Model model) {
        model.addAttribute("statuses", jdbc.queryForList("""
                SELECT status_id, requirement_status_name
                FROM requirement_status
                ORDER BY status_id
                """));

        model.addAttribute("types", jdbc.queryForList("""
                SELECT type_id, requirement_type_name
                FROM requirement_type
                WHERE type_is_active = 1
                ORDER BY requirement_type_name
                """));

        model.addAttribute("rejectionReasons", jdbc.queryForList("""
                SELECT rejection_reason_id, rejection_reason_name
                FROM rejection_reason
                WHERE rejection_reason_is_active = 1
                ORDER BY rejection_reason_name
                """));

        model.addAttribute("applications", jdbc.queryForList("""
                SELECT
                    a.application_id,
                    a.application_reference_number,
                    ap.applicant_first_name,
                    ap.applicant_last_name
                FROM application a
                JOIN applicant ap
                    ON ap.applicant_id = a.applicant_id
                WHERE COALESCE(ap.applicant_is_deleted, 0) = 0
                ORDER BY a.application_id DESC
                """));
    }

    private String nextTrackingNo() {
        Integer next = jdbc.queryForObject("""
                SELECT COALESCE(MAX(requirement_id), 0) + 1
                FROM requirement
                """, Integer.class);

        if (next == null) {
            next = 1;
        }

        return "DOC-" + Year.now().getValue() + "-" + String.format("%04d", next);
    }
}
