package com.pdts.controller;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class ApplicantPageController {

    private final JdbcTemplate jdbc;

    public ApplicantPageController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/applicants")
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String enrollment,
                       @RequestParam(required = false) String region,
                       Model model) {
        StringBuilder sql = new StringBuilder("""
                SELECT ap.applicant_id, ap.applicant_first_name, ap.applicant_last_name,
                       ap.applicant_email_address, ap.applicant_contact_number,
                       ap.applicant_region, ap.applicant_enrollment_status,
                       e.category_name, ap.applicant_created_at
                FROM applicant ap
                JOIN educational_background_category e ON e.category_id = ap.educational_background_category_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(ap.applicant_first_name) LIKE LOWER(?) OR LOWER(ap.applicant_last_name) LIKE LOWER(?) OR LOWER(ap.applicant_email_address) LIKE LOWER(?))");
            String q = "%" + search.trim() + "%";
            params.add(q); params.add(q); params.add(q);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND ap.educational_background_category_id = ?");
            params.add(category);
        }
        if (enrollment != null && !enrollment.isBlank()) {
            sql.append(" AND ap.applicant_enrollment_status = ?");
            params.add(enrollment);
        }
        if (region != null && !region.isBlank()) {
            sql.append(" AND LOWER(ap.applicant_region) LIKE LOWER(?)");
            params.add("%" + region.trim() + "%");
        }
        sql.append(" ORDER BY ap.applicant_created_at DESC, ap.applicant_id DESC");

        model.addAttribute("applicants", jdbc.queryForList(sql.toString(), params.toArray()));
        model.addAttribute("categories", jdbc.queryForList("SELECT category_id, category_name FROM educational_background_category ORDER BY category_name"));
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("region", region);
        return "applicants";
    }

    @GetMapping("/applicants/new")
    public String newForm(Model model) {
        addLookups(model);
        model.addAttribute("mode", "create");
        return "applicant-form";
    }

    @PostMapping("/applicants")
    public String create(@RequestParam Map<String, String> form, RedirectAttributes ra) {
        try {
            Integer applicantId = jdbc.queryForObject("""
                    INSERT INTO applicant (
                        applicant_first_name, applicant_middle_name, applicant_last_name, applicant_suffix,
                        applicant_sex, applicant_civil_status, applicant_house_number_street, applicant_barangay,
                        applicant_city_municipality, applicant_province, applicant_region, applicant_zip_code,
                        applicant_birth_date, applicant_email_address, applicant_contact_number,
                        educational_background_category_id, applicant_enrollment_status, user_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    RETURNING applicant_id
                    """, Integer.class,
                    required(form, "firstName"), blankToNull(form.get("middleName")), required(form, "lastName"), blankToNull(form.get("suffix")),
                    intValue(form, "sex", 1), intValue(form, "civilStatus", 1), blankToNull(form.get("street")), blankToNull(form.get("barangay")),
                    blankToNull(form.get("city")), blankToNull(form.get("province")), blankToNull(form.get("region")), blankToNull(form.get("zipCode")),
                    Date.valueOf(required(form, "birthDate")), required(form, "email"), required(form, "contactNumber"),
                    required(form, "categoryId"), required(form, "enrollmentStatus"));

            String referenceNo = nextApplicationReference();
            jdbc.update("""
                    INSERT INTO application (applicant_id, program_id, campus_id, application_status_id,
                                             application_date, application_semester, application_academic_year,
                                             application_reference_number)
                    VALUES (?, ?, ?, ?, CURRENT_DATE, ?, ?, ?)
                    """,
                    applicantId,
                    intValue(form, "programId", 1),
                    intValue(form, "campusId", 1),
                    intValue(form, "applicationStatusId", 1),
                    required(form, "semester"),
                    required(form, "academicYear"),
                    referenceNo);

            ra.addFlashAttribute("success", "Applicant created with application reference " + referenceNo + ".");
            return "redirect:/applicants/" + applicantId;
        } catch (DuplicateKeyException e) {
            ra.addFlashAttribute("error", "Email address or generated reference number already exists.");
            return "redirect:/applicants/new";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Create failed: " + e.getMessage());
            return "redirect:/applicants/new";
        }
    }

    @GetMapping("/applicants/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("applicant", one("""
                SELECT ap.*, e.category_name
                FROM applicant ap
                JOIN educational_background_category e ON e.category_id = ap.educational_background_category_id
                WHERE ap.applicant_id = ?
                """, id));
        model.addAttribute("applications", jdbc.queryForList("""
                SELECT a.*, p.program_name, p.program_code, c.campus_name, ast.application_status_name
                FROM application a
                JOIN program p ON p.program_id = a.program_id
                JOIN campus c ON c.campus_id = a.campus_id
                JOIN application_status ast ON ast.application_status_id = a.application_status_id
                WHERE a.applicant_id = ?
                ORDER BY a.application_date DESC, a.application_id DESC
                """, id));
        model.addAttribute("requirements", jdbc.queryForList("""
                SELECT r.*, rt.requirement_type_name, rs.requirement_status_name
                FROM requirement r
                JOIN requirement_type rt ON rt.type_id = r.requirement_type_id
                JOIN requirement_status rs ON rs.status_id = r.requirement_status_id
                JOIN application a ON a.application_id = r.application_id
                WHERE a.applicant_id = ?
                ORDER BY r.requirement_upload_date DESC
                """, id));
        return "applicant-detail";
    }

    @GetMapping("/applicants/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        addLookups(model);
        model.addAttribute("mode", "edit");
        model.addAttribute("applicant", one("SELECT * FROM applicant WHERE applicant_id = ?", id));
        return "applicant-form";
    }

    @PostMapping("/applicants/{id}/edit")
    public String update(@PathVariable Integer id, @RequestParam Map<String, String> form, RedirectAttributes ra) {
        jdbc.update("""
                UPDATE applicant SET
                    applicant_first_name = ?, applicant_middle_name = ?, applicant_last_name = ?, applicant_suffix = ?,
                    applicant_sex = ?, applicant_civil_status = ?, applicant_house_number_street = ?, applicant_barangay = ?,
                    applicant_city_municipality = ?, applicant_province = ?, applicant_region = ?, applicant_zip_code = ?,
                    applicant_birth_date = ?, applicant_email_address = ?, applicant_contact_number = ?,
                    educational_background_category_id = ?, applicant_enrollment_status = ?, applicant_updated_at = CURRENT_TIMESTAMP
                WHERE applicant_id = ?
                """,
                required(form, "firstName"), blankToNull(form.get("middleName")), required(form, "lastName"), blankToNull(form.get("suffix")),
                intValue(form, "sex", 1), intValue(form, "civilStatus", 1), blankToNull(form.get("street")), blankToNull(form.get("barangay")),
                blankToNull(form.get("city")), blankToNull(form.get("province")), blankToNull(form.get("region")), blankToNull(form.get("zipCode")),
                Date.valueOf(required(form, "birthDate")), required(form, "email"), required(form, "contactNumber"),
                required(form, "categoryId"), required(form, "enrollmentStatus"), id);
        ra.addFlashAttribute("success", "Applicant updated.");
        return "redirect:/applicants/" + id;
    }

    private void addLookups(Model model) {
        model.addAttribute("categories", jdbc.queryForList("SELECT category_id, category_name FROM educational_background_category ORDER BY category_name"));
        model.addAttribute("programs", jdbc.queryForList("SELECT program_id, program_name, program_code FROM program ORDER BY program_name"));
        model.addAttribute("campuses", jdbc.queryForList("SELECT campus_id, campus_name FROM campus ORDER BY campus_name"));
        model.addAttribute("applicationStatuses", jdbc.queryForList("SELECT application_status_id, application_status_name FROM application_status ORDER BY application_status_id"));
        model.addAttribute("defaultAcademicYear", Year.now().getValue() + "-" + (Year.now().getValue() + 1));
    }

    private Map<String, Object> one(String sql, Object... params) {
        return jdbc.queryForMap(sql, params);
    }

    private String nextApplicationReference() {
        Integer next = jdbc.queryForObject("SELECT COALESCE(MAX(application_id), 0) + 1 FROM application", Integer.class);
        if (next == null) next = 1;
        return "APP-" + Year.now().getValue() + "-" + String.format("%04d", next);
    }

    private String required(Map<String, String> form, String key) {
        String value = form.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required");
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer intValue(Map<String, String> form, String key, int defaultValue) {
        String value = form.get(key);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }
}
