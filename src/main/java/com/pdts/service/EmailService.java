// Path: src/main/java/com/pdts/service/EmailService.java
package com.pdts.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.Objects;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendDocumentStatusEmail(String toEmail, String studentName,
            String docType, String trackingNo,
            String status, String rejectionReason) {
        String safeEmail = Objects.requireNonNullElse(toEmail, "");
        String safeName = Objects.requireNonNullElse(studentName, "Student");
        String safeDoc = Objects.requireNonNullElse(docType, "Document");
        String safeTrack = Objects.requireNonNullElse(trackingNo, "—");
        String safeStatus = Objects.requireNonNullElse(status, "Updated");

        // FIX: rejectionReason was previously passed raw — now null-safe
        String safeRejection = Objects.requireNonNullElse(rejectionReason, "");

        if (safeEmail.isBlank())
            return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(new String[] { safeEmail });
            helper.setSubject("[PDTS] Document Status Update — " + safeDoc);

            // FIX: explicit ternary — null analyzers recognize != null check as @NonNull
            // guarantee
            String statusBody = buildStatusEmailBody(safeName, safeDoc, safeTrack, safeStatus, safeRejection);
            helper.setText(statusBody != null ? statusBody : "", true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send status email: " + e.getMessage());
        }
    }

    @Async
    public void sendTokenEmail(String toEmail, String studentName,
            String referenceNo, String plainToken) {
        String safeEmail = Objects.requireNonNullElse(toEmail, "");
        String safeName = Objects.requireNonNullElse(studentName, "Student");
        String safeRef = Objects.requireNonNullElse(referenceNo, "—");
        String safeToken = Objects.requireNonNullElse(plainToken, "—");

        if (safeEmail.isBlank())
            return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(new String[] { safeEmail });
            helper.setSubject("[PDTS] Your Document Tracking Access Token");

            // FIX: explicit ternary — null analyzers recognize != null check as @NonNull
            // guarantee
            String tokenBody = buildTokenEmailBody(safeName, safeRef, safeToken);
            helper.setText(tokenBody != null ? tokenBody : "", true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send token email: " + e.getMessage());
        }
    }

    @Async
    public void sendDeadlineReminderEmail(String toEmail, String studentName,
            String docType, long daysLeft) {
        String safeEmail = Objects.requireNonNullElse(toEmail, "");
        String safeName = Objects.requireNonNullElse(studentName, "Student");
        String safeDoc = Objects.requireNonNullElse(docType, "Document");

        if (safeEmail.isBlank())
            return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(safeEmail);
            helper.setSubject("[PDTS] Document Submission Reminder — " + daysLeft + " day(s) left");

            String urgency = daysLeft <= 1
                    ? "TODAY IS THE DEADLINE."
                    : daysLeft + " day(s) remaining.";

            String body = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;'>" +
                    "<div style='background:#8B0000;padding:20px;text-align:center;'>" +
                    "<h2 style='color:#fff;margin:0;'>PUP Open University System</h2>" +
                    "<p style='color:#ffcdd2;margin:4px 0 0;'>Office of the University Registrar</p>" +
                    "</div><div style='padding:24px;'>" +
                    "<p>Dear <strong>" + safeName + "</strong>,</p>" +
                    "<p>Reminder: your <strong>" + safeDoc + "</strong> deadline is approaching.</p>" +
                    "<p style='font-size:1.1em;color:#c62828;font-weight:bold;'>" + urgency + "</p>" +
                    "<p>Please submit immediately to the OUS Registrar's Office.</p>" +
                    "<br><p style='color:#555;font-size:.9em;'>PUP OUS — Document Tracking System</p>" +
                    "</div></div>";

            helper.setText(body, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send reminder email: " + e.getMessage());
        }
    }

    private String buildStatusEmailBody(String name, String docType, String trackingNo,
            String status, String rejectionReason) {
        String statusColor = getStatusColor(status);
        String rejectionBlock = "";
        if (rejectionReason != null && !rejectionReason.isBlank()) {
            rejectionBlock = "<div style='background:#fff8f8;border-left:4px solid #dc3545;padding:12px;margin:16px 0;'>"
                    +
                    "<strong>Rejection Reason:</strong><br>" + rejectionReason + "</div>";
        }
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;'>" +
                "<div style='background:#8B0000;padding:20px;text-align:center;'>" +
                "<h2 style='color:#fff;margin:0;'>PUP Open University System</h2>" +
                "<p style='color:#ffcdd2;margin:4px 0 0;'>Office of the University Registrar — PDTS</p>" +
                "</div><div style='padding:24px;'>" +
                "<p>Dear <strong>" + name + "</strong>,</p>" +
                "<p>Your document status has been updated:</p>" +
                "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>" +
                "<tr><td style='padding:8px;background:#f5f5f5;width:40%;'><strong>Document</strong></td>" +
                "<td style='padding:8px;'>" + docType + "</td></tr>" +
                "<tr><td style='padding:8px;background:#f5f5f5;'><strong>Tracking No.</strong></td>" +
                "<td style='padding:8px;font-family:monospace;'>" + trackingNo + "</td></tr>" +
                "<tr><td style='padding:8px;background:#f5f5f5;'><strong>Status</strong></td>" +
                "<td style='padding:8px;'><span style='background:" + statusColor +
                ";color:#fff;padding:4px 10px;border-radius:4px;font-size:.9em;'>" +
                status + "</span></td></tr></table>" + rejectionBlock +
                "<p>Visit the PUP OUS Document Status Portal to view your full status.</p>" +
                "<br><p style='color:#555;font-size:.9em;'>PUP OUS — Document Tracking System</p>" +
                "</div></div>";
    }

    private String buildTokenEmailBody(String name, String referenceNo, String plainToken) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;'>" +
                "<div style='background:#8B0000;padding:20px;text-align:center;'>" +
                "<h2 style='color:#fff;margin:0;'>PUP Open University System</h2>" +
                "<p style='color:#ffcdd2;margin:4px 0 0;'>Office of the University Registrar — PDTS</p>" +
                "</div><div style='padding:24px;'>" +
                "<p>Dear <strong>" + name + "</strong>,</p>" +
                "<p>Your application has been received. Use the details below to track your documents:</p>" +
                "<div style='background:#f9f9f9;border:1px solid #ddd;border-radius:8px;padding:20px;margin:16px 0;text-align:center;'>"
                +
                "<p style='margin:0 0 8px;color:#555;'>Application Reference Number</p>" +
                "<p style='font-size:1.4em;font-weight:bold;font-family:monospace;color:#333;margin:0 0 16px;'>" +
                referenceNo + "</p>" +
                "<p style='margin:0 0 8px;color:#555;'>Access Token</p>" +
                "<p style='font-size:1.1em;font-weight:bold;font-family:monospace;color:#8B0000;margin:0;word-break:break-all;'>"
                +
                plainToken + "</p></div>" +
                "<p><strong>Important:</strong> Keep this token private. It expires in 180 days.</p>" +
                "<br><p style='color:#555;font-size:.9em;'>PUP OUS — Document Tracking System</p>" +
                "</div></div>";
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "Verified/Received" -> "#28a745";
            case "Rejected" -> "#dc3545";
            case "Under Review" -> "#2e75b6";
            case "For Resubmission" -> "#c8a951";
            default -> "#ffa500";
        };
    }
}
