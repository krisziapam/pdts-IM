package com.pdts.controller;

import com.pdts.service.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UtilityPageController {

    private final JdbcTemplate jdbc;
    private final AuditLogService auditLogService;

    public UtilityPageController(JdbcTemplate jdbc, AuditLogService auditLogService) {
        this.jdbc = jdbc;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/email-notifications")
    public String emailNotifications(Model model) {
        model.addAttribute("simulatedEmailLogs", jdbc.queryForList("""
                SELECT 
                    user_activity_log_id,
                    user_activity_log_description,
                    user_activity_log_new_value,
                    user_activity_log_performed_at
                FROM user_activity_log
                WHERE user_activity_log_action_type = 'SEND_EMAIL'
                ORDER BY user_activity_log_performed_at DESC
                LIMIT 10
                """));

        return "email-notifications";
    }

    @PostMapping("/email-notifications/send")
    public String sendEmailNotification(@RequestParam String recipient,
                                        @RequestParam String emailType,
                                        @RequestParam String subject,
                                        @RequestParam String messageBody,
                                        @RequestParam(required = false) String remarks,
                                        RedirectAttributes ra) {

        String cleanRemarks = remarks == null || remarks.isBlank()
                ? "No additional remarks"
                : remarks.trim();

        auditLogService.log(
                "SEND_EMAIL",
                "email_log",
                null,
                "Sent " + emailType + " to " + recipient,
                null,
                "Subject: " + subject + " | Remarks: " + cleanRemarks
        );

        ra.addFlashAttribute("success", "Email notification marked as sent and recorded in Audit Trail.");
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
}
