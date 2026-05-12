package com.pdts.controller;

import com.pdts.service.AuditLogService;
import com.pdts.service.EmailService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UtilityPageController {

    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final JdbcTemplate jdbc;

    public UtilityPageController(
            AuditLogService auditLogService,
            EmailService emailService,
            JdbcTemplate jdbc
    ) {
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.jdbc = jdbc;
    }

    @GetMapping("/email-notifications")
    public String emailNotifications(Model model) {

        model.addAttribute("recipients", jdbc.queryForList("""
            SELECT
                ap.applicant_id,
                ap.applicant_first_name || ' ' || ap.applicant_last_name AS full_name,
                ap.applicant_email_address AS email,
                '' AS reference_no
            FROM applicant ap
            WHERE ap.applicant_email_address IS NOT NULL
              AND TRIM(ap.applicant_email_address) <> ''
            ORDER BY ap.applicant_created_at DESC, ap.applicant_id DESC
        """));

        model.addAttribute("emailLogs", jdbc.queryForList("""
            SELECT
                user_activity_log_id,
                user_activity_log_description,
                user_activity_log_new_value,
                user_activity_log_performed_at
            FROM user_activity_log
            WHERE user_activity_log_action_type = 'SEND_EMAIL'
            ORDER BY user_activity_log_performed_at DESC
            LIMIT 20
        """));

        return "email-notifications";
    }

    @PostMapping("/email-notifications/send")
    public String sendEmailNotification(
            @RequestParam String recipient,
            @RequestParam String emailType,
            @RequestParam String subject,
            @RequestParam("body") String body,
            @RequestParam(required = false) String remarks,
            RedirectAttributes ra
    ) {
        try {
            String emailTypeLabel = getEmailTypeLabel(emailType);
            String cleanRemarks = remarks == null || remarks.isBlank()
                    ? "No additional remarks"
                    : remarks.trim();

            emailService.sendManualEmail(recipient, subject, body, cleanRemarks);

            auditLogService.log(
                    "SEND_EMAIL",
                    "email_log",
                    null,
                    "Sent " + emailTypeLabel + " to " + recipient,
                    null,
                    "Subject: " + subject + " | Remarks: " + cleanRemarks
            );

            ra.addFlashAttribute("success", "Email notification sent successfully.");

        } catch (Exception e) {
            e.printStackTrace();

            String message = e.getMessage();
            if (message == null || message.isBlank()) {
                message = e.getClass().getSimpleName();
            }

            ra.addFlashAttribute("error", "Email failed: " + message);
        }

        return "redirect:/email-notifications";
    }

    @GetMapping("/tracking-lookup")
    public String trackingLookup() {
        return "tracking-lookup";
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String curriculum,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String status,
            Model model
    ) {
        String cleanRegion = clean(region);
        String cleanCurriculum = clean(curriculum);
        String cleanProgram = clean(program);
        String cleanStatus = clean(status);

        ReportQuery query = buildReportQuery(cleanRegion, cleanCurriculum, cleanProgram, cleanStatus);

        String baseFrom = baseReportFrom();

        int filteredStudents = queryCount(
                "SELECT COUNT(DISTINCT ap.applicant_id) " + baseFrom + query.where,
                query.params
        );

        int clearedStudents = queryCount(
                "SELECT COUNT(DISTINCT ap.applicant_id) " + baseFrom + query.where + clearedCondition(),
                query.params
        );

        int pendingRequirements = queryCount(
                "SELECT COUNT(*) " +
                        baseFrom +
                        " JOIN requirement r ON r.application_id = latest_app.application_id " +
                        " JOIN requirement_status rs ON rs.status_id = r.requirement_status_id " +
                        query.where +
                        " AND rs.requirement_status_name <> 'Verified/Received' ",
                query.params
        );

        List<Map<String, Object>> curriculumBreakdown = toProgressRows(
                jdbc.queryForList(
                        """
                        SELECT
                            ebc.category_name AS label,
                            COUNT(DISTINCT ap.applicant_id) AS count
                        """ + baseFrom + query.where + """
                        GROUP BY ebc.category_name
                        ORDER BY count DESC, ebc.category_name ASC
                        """,
                        query.params.toArray()
                ),
                false
        );

        List<Map<String, Object>> regionBreakdown = toProgressRows(
                jdbc.queryForList(
                        """
                        SELECT
                            COALESCE(NULLIF(TRIM(ap.applicant_region), ''), 'Unspecified') AS label,
                            COUNT(DISTINCT ap.applicant_id) AS count
                        """ + baseFrom + query.where + """
                        GROUP BY COALESCE(NULLIF(TRIM(ap.applicant_region), ''), 'Unspecified')
                        ORDER BY count DESC, label ASC
                        """,
                        query.params.toArray()
                ),
                true
        );

        model.addAttribute("regions", jdbc.queryForList("""
            SELECT DISTINCT applicant_region
            FROM applicant
            WHERE applicant_region IS NOT NULL
              AND TRIM(applicant_region) <> ''
              AND COALESCE(applicant_is_deleted, 0) = 0
            ORDER BY applicant_region
        """, String.class));

        model.addAttribute("curricula", jdbc.queryForList("""
            SELECT category_id, category_name
            FROM educational_background_category
            ORDER BY category_name
        """));

        model.addAttribute("programs", jdbc.queryForList("""
            SELECT program_id, program_code, program_name
            FROM program
            ORDER BY program_code, program_name
        """));

        model.addAttribute("selectedRegion", cleanRegion);
        model.addAttribute("selectedCurriculum", cleanCurriculum);
        model.addAttribute("selectedProgram", cleanProgram);
        model.addAttribute("selectedStatus", cleanStatus);

        model.addAttribute("filteredStudents", filteredStudents);
        model.addAttribute("clearedStudents", clearedStudents);
        model.addAttribute("pendingRequirements", pendingRequirements);
        model.addAttribute("curriculumBreakdown", curriculumBreakdown);
        model.addAttribute("regionBreakdown", regionBreakdown);

        return "reports";
    }

    private ReportQuery buildReportQuery(String region, String curriculum, String program, String status) {
        StringBuilder where = new StringBuilder(" WHERE COALESCE(ap.applicant_is_deleted, 0) = 0 ");
        List<Object> params = new ArrayList<>();

        if (!region.isBlank()) {
            where.append(" AND ap.applicant_region = ? ");
            params.add(region);
        }

        if (!curriculum.isBlank()) {
            where.append(" AND ap.educational_background_category_id = ? ");
            params.add(curriculum);
        }

        if (!program.isBlank()) {
            where.append(" AND latest_app.program_id = ? ");
            params.add(Integer.parseInt(program));
        }

        if ("continuing".equals(status) || "on_leave".equals(status)) {
            where.append(" AND ap.applicant_enrollment_status = ? ");
            params.add(status);
        }

        if ("cleared".equals(status)) {
            where.append(clearedCondition());
        }

        if ("pending".equals(status)) {
            where.append("""
                AND EXISTS (
                    SELECT 1
                    FROM requirement r_pending
                    JOIN requirement_status rs_pending
                      ON rs_pending.status_id = r_pending.requirement_status_id
                    WHERE r_pending.application_id = latest_app.application_id
                      AND rs_pending.requirement_status_name <> 'Verified/Received'
                )
            """);
        }

        return new ReportQuery(where.toString(), params);
    }

    private String baseReportFrom() {
        return """
            FROM applicant ap
            JOIN educational_background_category ebc
              ON ebc.category_id = ap.educational_background_category_id
            LEFT JOIN LATERAL (
                SELECT a.*
                FROM application a
                WHERE a.applicant_id = ap.applicant_id
                ORDER BY a.application_date DESC, a.application_id DESC
                LIMIT 1
            ) latest_app ON TRUE
            LEFT JOIN program p
              ON p.program_id = latest_app.program_id
        """;
    }

    private String clearedCondition() {
        return """
            AND latest_app.application_id IS NOT NULL
            AND EXISTS (
                SELECT 1
                FROM requirement r0
                WHERE r0.application_id = latest_app.application_id
            )
            AND NOT EXISTS (
                SELECT 1
                FROM requirement r1
                JOIN requirement_status rs1
                  ON rs1.status_id = r1.requirement_status_id
                WHERE r1.application_id = latest_app.application_id
                  AND rs1.requirement_status_name <> 'Verified/Received'
            )
        """;
    }

    private int queryCount(String sql, List<Object> params) {
        Number count = jdbc.queryForObject(sql, Number.class, params.toArray());
        return count == null ? 0 : count.intValue();
    }

    private List<Map<String, Object>> toProgressRows(List<Map<String, Object>> rows, boolean regionRows) {
        int max = 0;
        for (Map<String, Object> row : rows) {
            max = Math.max(max, toInt(row.get("count")));
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            int count = toInt(row.get("count"));
            int percent = max == 0 ? 0 : Math.max(6, Math.round((count * 100f) / max));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", String.valueOf(row.get("label")));
            item.put("count", count);
            item.put("percent", percent);
            item.put("cssClass", regionRows ? "region" : curriculumCssClass(String.valueOf(row.get("label"))));
            result.add(item);
        }

        return result;
    }

    private String curriculumCssClass(String label) {
        String value = label == null ? "" : label.toLowerCase();

        if (value.contains("als")) {
            return "als";
        }
        if (value.contains("college")) {
            return "college";
        }
        if (value.contains("old")) {
            return "old";
        }
        if (value.contains("senior")) {
            return "shs";
        }
        return "";
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ReportQuery {
        private final String where;
        private final List<Object> params;

        private ReportQuery(String where, List<Object> params) {
            this.where = where;
            this.params = params;
        }
    }

    private String getEmailTypeLabel(String emailType) {
        if ("pending".equals(emailType)) {
            return "Pending Requirements Reminder";
        }
        if ("received".equals(emailType)) {
            return "Acknowledgment Receipt";
        }
        if ("rejected".equals(emailType)) {
            return "Document Rejection Notice";
        }
        if ("deadline".equals(emailType)) {
            return "Deadline Alert";
        }
        return "Email Notification";
    }
}
