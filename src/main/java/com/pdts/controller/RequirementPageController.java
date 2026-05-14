package com.pdts.controller;

import com.pdts.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class RequirementPageController {

    private final JdbcTemplate jdbc;
    private final AuditLogService auditLogService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final String supabaseUrl = System.getenv("SUPABASE_URL");
    private final String supabaseServiceRoleKey = System.getenv("SUPABASE_SERVICE_ROLE_KEY");
    private final String supabaseBucket = System.getenv().getOrDefault("SUPABASE_BUCKET", "requirement-documents");

    private final String resendApiKey = System.getenv("RESEND_API_KEY");
    private final String resendFromEmail = System.getenv("RESEND_FROM_EMAIL");
    private final String appBaseUrl = System.getenv().getOrDefault("APP_BASE_URL", "https://pdts-im.onrender.com");

    public RequirementPageController(JdbcTemplate jdbc, AuditLogService auditLogService) {
        this.jdbc = jdbc;
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
                    r.requirement_remarks,
                    r.requirement_file_url,
                    r.requirement_storage_path,
                    rt.requirement_type_name,
                    rs.requirement_status_name,
                    a.application_reference_number,
                    ap.applicant_id,
                    ap.applicant_first_name,
                    ap.applicant_last_name,
                    ap.applicant_email_address
                FROM requirement r
                JOIN requirement_type rt ON rt.type_id = r.requirement_type_id
                JOIN requirement_status rs ON rs.status_id = r.requirement_status_id
                JOIN application a ON a.application_id = r.application_id
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
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

        try {
            validateSupabaseConfig();

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Please choose a file to upload.");
            }

            Integer activeApplicationCount = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM application a
                    JOIN applicant ap ON ap.applicant_id = a.applicant_id
                    WHERE a.application_id = ?
                      AND COALESCE(ap.applicant_is_deleted, 0) = 0
                    """, Integer.class, applicationId);

            if (activeApplicationCount == null || activeApplicationCount == 0) {
                throw new IllegalArgumentException("Selected application does not belong to an active applicant.");
            }

            String originalName = cleanFileName(file.getOriginalFilename());
            String storagePath = buildStoragePath(applicationId, originalName);
            String publicUrl = uploadToSupabase(file, storagePath);
            String trackingNo = nextTrackingNo();

            jdbc.update("""
                    INSERT INTO requirement (
                        application_id,
                        requirement_type_id,
                        requirement_status_id,
                        requirement_tracking_no,
                        requirement_file_name,
                        requirement_image_path,
                        requirement_file_url,
                        requirement_storage_path,
                        requirement_uploaded_by_user_id
                    )
                    VALUES (?, ?, 1, ?, ?, ?, ?, ?, 1)
                    """,
                    applicationId,
                    requirementTypeId,
                    trackingNo,
                    originalName,
                    publicUrl,
                    publicUrl,
                    storagePath
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
                    "Stored in Supabase Storage"
            );

            ra.addFlashAttribute("success", "Document uploaded. Tracking number: " + trackingNo);
            return "redirect:/requirements";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Upload failed: " + e.getMessage());
            return "redirect:/requirements/new";
        }
    }

    @GetMapping("/requirements/{id}/view")
    public String viewDocument(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            String fileUrl = jdbc.queryForObject("""
                    SELECT requirement_file_url
                    FROM requirement
                    WHERE requirement_id = ?
                    """, String.class, id);

            if (fileUrl == null || fileUrl.isBlank()) {
                ra.addFlashAttribute("error", "No document URL found. Please re-upload this document.");
                return "redirect:/requirements";
            }

            return "redirect:" + fileUrl;

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Document not found or unavailable.");
            return "redirect:/requirements";
        }
    }

    @PostMapping("/requirements/{id}/delete")
    public String deleteRequirement(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            Map<String, Object> requirement = jdbc.queryForMap("""
                    SELECT requirement_file_name, requirement_tracking_no, requirement_storage_path
                    FROM requirement
                    WHERE requirement_id = ?
                    """, id);

            String trackingNo = String.valueOf(requirement.get("requirement_tracking_no"));
            String fileName = String.valueOf(requirement.get("requirement_file_name"));
            String storagePath = stringOrNull(requirement.get("requirement_storage_path"));

            if (storagePath != null && !storagePath.isBlank()) {
                deleteFromSupabase(storagePath);
            }

            jdbc.update("""
                    DELETE FROM requirement
                    WHERE requirement_id = ?
                    """, id);

            auditLogService.log(
                    "DELETE_DOCUMENT",
                    "requirement",
                    id.longValue(),
                    "Deleted document " + fileName,
                    trackingNo,
                    "Removed from Supabase Storage"
            );

            ra.addFlashAttribute("success", "Document deleted successfully.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }

        return "redirect:/requirements";
    }

    @PostMapping("/requirements/{id}/replace")
    public String replaceRequirement(@PathVariable Integer id,
                                     @RequestParam MultipartFile file,
                                     RedirectAttributes ra) {

        try {
            validateSupabaseConfig();

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Please choose a file.");
            }

            Map<String, Object> requirement = jdbc.queryForMap("""
                    SELECT application_id, requirement_tracking_no, requirement_storage_path
                    FROM requirement
                    WHERE requirement_id = ?
                    """, id);

            Integer applicationId = ((Number) requirement.get("application_id")).intValue();
            String oldStoragePath = stringOrNull(requirement.get("requirement_storage_path"));
            String trackingNo = String.valueOf(requirement.get("requirement_tracking_no"));

            if (oldStoragePath != null && !oldStoragePath.isBlank()) {
                deleteFromSupabase(oldStoragePath);
            }

            String originalName = cleanFileName(file.getOriginalFilename());
            String newStoragePath = buildStoragePath(applicationId, originalName);
            String publicUrl = uploadToSupabase(file, newStoragePath);

            jdbc.update("""
                    UPDATE requirement
                    SET requirement_file_name = ?,
                        requirement_image_path = ?,
                        requirement_file_url = ?,
                        requirement_storage_path = ?,
                        requirement_upload_date = CURRENT_TIMESTAMP,
                        requirement_status_id = 1,
                        requirement_date_received = NULL,
                        requirement_processed_by_user_id = NULL,
                        requirement_processed_at = NULL,
                        rejection_reason_id = NULL,
                        rejection_reason_rejected_by_user_id = NULL,
                        rejection_reason_rejected_at = NULL,
                        requirement_remarks = NULL
                    WHERE requirement_id = ?
                    """,
                    originalName,
                    publicUrl,
                    publicUrl,
                    newStoragePath,
                    id
            );

            auditLogService.log(
                    "REUPLOAD_DOCUMENT",
                    "requirement",
                    id.longValue(),
                    "Re-uploaded document " + originalName,
                    trackingNo,
                    "Stored in Supabase Storage. Status reset to Pending"
            );

            ra.addFlashAttribute("success", "Document replaced successfully.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Replace failed: " + e.getMessage());
        }

        return "redirect:/requirements";
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
                    JOIN requirement_type rt ON rt.type_id = r.requirement_type_id
                    JOIN requirement_status rs ON rs.status_id = r.requirement_status_id
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

            sendRequirementStatusEmailSafe(id, newStatus, trackingNo, documentType);

            ra.addFlashAttribute("success", "Requirement status updated.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Status update failed: " + e.getMessage());
        }

        return "redirect:/requirements";
    }

    private String uploadToSupabase(MultipartFile file, String storagePath) throws Exception {
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + encodeStoragePath(storagePath);

        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + supabaseServiceRoleKey)
                .header("apikey", supabaseServiceRoleKey)
                .header("Content-Type", contentType)
                .header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Supabase upload failed: " + response.body());
        }

        return supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + encodeStoragePath(storagePath);
    }

    private void deleteFromSupabase(String storagePath) throws Exception {
        String deleteUrl = supabaseUrl + "/storage/v1/object/" + supabaseBucket;

        String body = "{\"prefixes\":[\"" + storagePath.replace("\"", "\\\"") + "\"]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .header("Authorization", "Bearer " + supabaseServiceRoleKey)
                .header("apikey", supabaseServiceRoleKey)
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != HttpStatus.OK.value()
                && response.statusCode() != HttpStatus.NO_CONTENT.value()
                && response.statusCode() != HttpStatus.NOT_FOUND.value()) {
            throw new IllegalStateException("Supabase delete failed: " + response.body());
        }
    }

    private String buildStoragePath(Integer applicationId, String originalName) {
        String ext = ".pdf";

        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        return "applications/" + applicationId + "/" + UUID.randomUUID() + ext;
    }

    private String cleanFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "document.pdf";
        }

        return fileName.replaceAll("[\\\\/]", "_").trim();
    }

    private String encodeStoragePath(String path) {
        String[] parts = path.split("/");
        List<String> encoded = new ArrayList<>();

        for (String part : parts) {
            encoded.add(URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"));
        }

        return String.join("/", encoded);
    }

    private void validateSupabaseConfig() {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new IllegalStateException("SUPABASE_URL is missing in environment variables.");
        }

        if (supabaseServiceRoleKey == null || supabaseServiceRoleKey.isBlank()) {
            throw new IllegalStateException("SUPABASE_SERVICE_ROLE_KEY is missing in environment variables.");
        }

        if (supabaseBucket == null || supabaseBucket.isBlank()) {
            throw new IllegalStateException("SUPABASE_BUCKET is missing in environment variables.");
        }
    }

    private String stringOrNull(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value);
        return text.equalsIgnoreCase("null") ? null : text;
    }

    private void sendRequirementStatusEmailSafe(Integer requirementId,
                                                String newStatus,
                                                String trackingNo,
                                                String documentType) {
        try {
            if (resendApiKey == null || resendApiKey.isBlank()
                    || resendFromEmail == null || resendFromEmail.isBlank()) {
                return;
            }

            Map<String, Object> info = jdbc.queryForMap("""
                    SELECT
                        ap.applicant_first_name,
                        ap.applicant_last_name,
                        ap.applicant_email_address,
                        a.application_reference_number
                    FROM requirement r
                    JOIN application a ON a.application_id = r.application_id
                    JOIN applicant ap ON ap.applicant_id = a.applicant_id
                    WHERE r.requirement_id = ?
                    """, requirementId);

            String firstName = String.valueOf(info.get("applicant_first_name"));
            String lastName = String.valueOf(info.get("applicant_last_name"));
            String email = String.valueOf(info.get("applicant_email_address"));
            String referenceNo = String.valueOf(info.get("application_reference_number"));

            String trackingUrl = appBaseUrl + "/tracking-lookup?trackingNo="
                    + URLEncoder.encode(referenceNo, StandardCharsets.UTF_8);

            String subject = "PUP Document Tracking Status Update - " + referenceNo;

           String statusMessage = getStatusEmailMessage(newStatus);

String html = """
        <div style="font-family:Arial,sans-serif;color:#222;line-height:1.6;">
            <h2 style="color:#8B0000;">Document Status Update</h2>

            <p>Dear %s %s,</p>

            <p>%s</p>

            <p><strong>Application Reference Number:</strong> %s</p>
            <p><strong>Document Tracking Number:</strong> %s</p>
            <p><strong>Document Type:</strong> %s</p>
            <p><strong>Current Status:</strong> %s</p>

            <p>
                <a href="%s" style="background:#8B0000;color:white;padding:12px 18px;text-decoration:none;border-radius:8px;display:inline-block;">
                    Track Application
                </a>
            </p>

            <p>Thank you.</p>

            <p style="color:#666;font-size:13px;">
                PUP Registrar PDTS System
            </p>
        </div>
        """.formatted(
        escapeJson(firstName),
        escapeJson(lastName),
        statusMessage,
        escapeJson(referenceNo),
        escapeJson(trackingNo),
        escapeJson(documentType),
        escapeJson(newStatus),
        trackingUrl
);

            
            sendEmail(email, subject, html);

        } catch (Exception ignored) {
            // Status update must not fail if email sending fails.
        }
    }

    private String getStatusEmailMessage(String status) {

    if ("Verified/Received".equalsIgnoreCase(status)) {
        return "Your submitted document has been verified and received. You may continue tracking your application status through the tracking portal.";
    }

    if ("Under Review".equalsIgnoreCase(status)) {
        return "Your submitted document is still under review. Please continue checking the tracking portal for further updates.";
    }

    if ("For Resubmission".equalsIgnoreCase(status)) {
        return "Your submitted document requires resubmission. Please review the remarks or instructions in the tracking portal, prepare the corrected document, and re-upload it for further evaluation.";
    }

    if ("Rejected".equalsIgnoreCase(status)) {
        return "Your submitted document was rejected. Please check if the uploaded document is correct, clear, complete, and matches the required document type. Kindly prepare the correct file and resubmit it through the system.";
    }

    return "Your submitted document status has been updated. Please check the tracking portal for details.";
}

    private void sendEmail(String toEmail, String subject, String html) throws Exception {
        String body = """
                {
                  "from": "%s",
                  "to": ["%s"],
                  "subject": "%s",
                  "html": "%s"
                }
                """.formatted(
                escapeJson(resendFromEmail),
                escapeJson(toEmail),
                escapeJson(subject),
                escapeJson(html)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String getRequirementActionType(Integer statusId) {
        if (statusId == 3) return "RECEIVE_DOCUMENT";
        if (statusId == 4) return "REJECT_DOCUMENT";
        if (statusId == 5) return "FOR_RESUBMISSION";
        if (statusId == 2) return "UNDER_REVIEW";
        return "UPDATE_DOCUMENT_STATUS";
    }

    private boolean requirementBelongsToActiveApplicant(Integer requirementId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM requirement r
                JOIN application a ON a.application_id = r.application_id
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
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
            ap.applicant_last_name,
            ap.applicant_educational_background
                FROM application a
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
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
