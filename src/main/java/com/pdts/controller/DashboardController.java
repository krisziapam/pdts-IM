package com.pdts.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final JdbcTemplate jdbc;

    public DashboardController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("userCount", count("SELECT COUNT(*) FROM app_user"));
        model.addAttribute("activeUserCount", count("SELECT COUNT(*) FROM app_user WHERE user_is_active = 1"));

        model.addAttribute("applicantCount", count("""
                SELECT COUNT(*)
                FROM applicant
                WHERE COALESCE(applicant_is_deleted, 0) = 0
                """));

        model.addAttribute("applicationCount", count("""
                SELECT COUNT(*)
                FROM application a
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
                WHERE COALESCE(ap.applicant_is_deleted, 0) = 0
                """));

        model.addAttribute("requirementCount", count("""
                SELECT COUNT(*)
                FROM requirement r
                JOIN application a ON a.application_id = r.application_id
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
                WHERE COALESCE(ap.applicant_is_deleted, 0) = 0
                """));

        model.addAttribute("pendingCount", count("""
                SELECT COUNT(*)
                FROM requirement r
                JOIN application a ON a.application_id = r.application_id
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
                WHERE r.requirement_status_id = 1
                  AND COALESCE(ap.applicant_is_deleted, 0) = 0
                """));

        model.addAttribute("underReviewCount", count("""
                SELECT COUNT(*)
                FROM requirement r
                JOIN application a ON a.application_id = r.application_id
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
                WHERE r.requirement_status_id = 2
                  AND COALESCE(ap.applicant_is_deleted, 0) = 0
                """));

        model.addAttribute("verifiedCount", count("""
                SELECT COUNT(*)
                FROM requirement r
                JOIN application a ON a.application_id = r.application_id
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
                WHERE r.requirement_status_id = 3
                  AND COALESCE(ap.applicant_is_deleted, 0) = 0
                """));

        model.addAttribute("rejectedCount", count("""
                SELECT COUNT(*)
                FROM requirement r
                JOIN application a ON a.application_id = r.application_id
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
                WHERE r.requirement_status_id = 4
                  AND COALESCE(ap.applicant_is_deleted, 0) = 0
                """));

        model.addAttribute("resubmissionCount", count("""
                SELECT COUNT(*)
                FROM requirement r
                JOIN application a ON a.application_id = r.application_id
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
                WHERE r.requirement_status_id = 5
                  AND COALESCE(ap.applicant_is_deleted, 0) = 0
                """));

        model.addAttribute("recentApplicants", jdbc.queryForList("""
                SELECT applicant_id,
                       applicant_first_name,
                       applicant_last_name,
                       applicant_email_address,
                       applicant_region,
                       applicant_enrollment_status,
                       applicant_created_at
                FROM applicant
                WHERE COALESCE(applicant_is_deleted, 0) = 0
                ORDER BY applicant_created_at DESC, applicant_id DESC
                LIMIT 8
                """));

        model.addAttribute("recentRequirements", jdbc.queryForList("""
                SELECT r.requirement_id,
                       r.requirement_tracking_no,
                       r.requirement_file_name,
                       r.requirement_upload_date,
                       rt.requirement_type_name,
                       rs.requirement_status_name,
                       ap.applicant_first_name,
                       ap.applicant_last_name
                FROM requirement r
                JOIN requirement_type rt ON rt.type_id = r.requirement_type_id
                JOIN requirement_status rs ON rs.status_id = r.requirement_status_id
                JOIN application a ON a.application_id = r.application_id
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
                WHERE COALESCE(ap.applicant_is_deleted, 0) = 0
                ORDER BY r.requirement_upload_date DESC, r.requirement_id DESC
                LIMIT 8
                """));

        model.addAttribute("recentApplications", jdbc.queryForList("""
                SELECT a.application_id,
                       a.application_reference_number,
                       a.application_date,
                       ast.application_status_name,
                       p.program_code,
                       p.program_name,
                       ap.applicant_first_name,
                       ap.applicant_last_name
                FROM application a
                JOIN applicant ap ON ap.applicant_id = a.applicant_id
                JOIN program p ON p.program_id = a.program_id
                JOIN application_status ast ON ast.application_status_id = a.application_status_id
                WHERE COALESCE(ap.applicant_is_deleted, 0) = 0
                ORDER BY a.application_date DESC, a.application_id DESC
                LIMIT 8
                """));

        return "dashboard";
    }

    private Integer count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
