package com.pdts.controller;

import com.pdts.service.AuditLogService;
import com.pdts.service.EmailService;
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
                COALESCE(latest_app.application_reference_number, '') AS reference_no
            FROM applicant ap
            LEFT JOIN LATERAL (
                SELECT a.application_reference_number
                FROM application a
                WHERE a.applicant_id = ap.applicant_id
                ORDER BY a.application_date DESC, a.application_id DESC
                LIMIT 1
            ) latest_app ON TRUE
            WHERE COALESCE(ap.applicant_is_deleted, 0) = 0
              AND ap.applicant_email_address IS NOT NULL
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
    public String reports() {
        return "reports";
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
