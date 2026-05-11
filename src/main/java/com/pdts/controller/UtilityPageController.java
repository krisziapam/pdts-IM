package com.pdts.controller;

import com.pdts.service.AuditLogService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UtilityPageController {

    private final AuditLogService auditLogService;

    public UtilityPageController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/email-notifications")
    public String emailNotifications() {
        return "email-notifications";
    }

    @PostMapping("/email-notifications/send")
    public String sendEmailNotification(@RequestParam String recipient,
                                        @RequestParam String emailType,
                                        @RequestParam String subject,
                                        @RequestParam("body") String body,
                                        @RequestParam(required = false) String remarks,
                                        RedirectAttributes ra) {

        String emailTypeLabel = getEmailTypeLabel(emailType);

        String cleanRemarks = remarks == null || remarks.isBlank()
                ? "No additional remarks"
                : remarks.trim();

        auditLogService.log(
                "SEND_EMAIL",
                "email_log",
                null,
                "Sent " + emailTypeLabel + " to " + recipient,
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
