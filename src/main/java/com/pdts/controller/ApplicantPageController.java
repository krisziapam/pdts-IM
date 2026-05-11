package com.pdts.controller;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
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
                SELECT
                    ap.applicant_id,
                    ap.applicant_first_name,
                    ap.applicant_last_name,
                    ap.applicant_email_address,
                    ap.applicant_contact_number,
                    ap.applicant_region,
                    ap.applicant_enrollment_status,
                    ap.applicant_created_at,
                    e.category_id,
                    e.category_name,
                    latest_app.application_reference_number,
                    latest_app.program_code,
                    latest_app.program_name,
                    COALESCE(
                        REPLACE(latest_app.application_reference_number, 'APP-', 'STU-'),
                        'STU-' || ap.applicant_id
                    ) AS student_tracking_no
                FROM applicant ap
                JOIN educational_background_category e
                    ON e.category_id = ap.educational_background_category_id
                LEFT JOIN LATERAL (
                    SELECT
                        a.application_id,
                        a.application_reference_number,
                        p.program_code,
                        p.program_name
                    FROM application a
                    LEFT JOIN program p
                        ON p.program_id = a.program_id
                    WHERE a.applicant_id = ap.applicant_id
                    ORDER BY a.application_date DESC, a.application_id DESC
                    LIMIT 1
                ) latest_app ON TRUE
                WHERE COALESCE(ap.applicant_is_deleted, 0) = 0
                """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (
                        LOWER(ap.applicant_first_name) LIKE LOWER(?)
                        OR LOWER(ap.applicant_last_name) LIKE LOWER(?)
                        OR LOWER(ap.applicant_email_address) LIKE LOWER(?)
                        OR LOWER(COALESCE(ap.applicant_contact_number, '')) LIKE LOWER(?)
                        OR LOWER(COALESCE(latest_app.application_reference_number, '')) LIKE LOWER(?)
                        OR LOWER(COALESCE(REPLACE(latest_app.application_reference_number, 'APP-', 'STU-'), '')) LIKE LOWER(?)
                    )
                    """);

            String q = "%" + search.trim() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (category != null && !category.isBlank()) {
            sql.append(" AND ap.educational_background_category_id = ?");
            params.add(category.trim());
        }

        if (enrollment != null && !enrollment.isBlank()) {
            sql.append(" AND ap.applicant_enrollment_status = ?");
            params.add(enrollment.trim());
        }

        if (region != null && !region.isBlank()) {
            sql.append(" AND LOWER(ap.applicant_region) LIKE LOWER(?)");
            params.add("%" + region.trim() + "%");
        }

        sql.append(" ORDER BY ap.applicant_created_at DESC, ap.applicant_id DESC");

        model.addAttribute("applicants", jdbc.queryForList(sql.toString(), params.toArray()));

        model.addAttribute("categories", jdbc.queryForList("""
                SELECT category_id, category_name
                FROM educational_background_category
                ORDER BY category_name
                """));

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

    @Transactional
    @PostMapping("/applicants")
    public String create(@RequestParam Map<String, String> form, RedirectAttributes ra) {
        try {
            Integer applicantId = jdbc.queryForObject("""
                    INSERT INTO applicant (
                        applicant_first_name,
                        applicant_middle_name,
                        applicant_last_name,
                        applicant_suffix,
                        applicant_sex,
                        applicant_civil_status,
                        applicant_house_number_street,
                        applicant_barangay,
                        applicant_city_municipality,
                        applicant_province,
                        applicant_region,
                        applicant_zip_code,
                        applicant_birth_date,
                        applicant_email_address,
                        applicant_contact_number,
                        educational_background_category_id,
                        applicant_enrollment_status,
                        user_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    RETURNING applicant_id
                    """,
                    Integer.class,
                    required(form, "firstName"),
                    blankToNull(form.get("middleName")),
                    required(form, "lastName"),
                    blankToNull(form.get("suffix")),
                    intValue(form, "sex", 1),
                    intValue(form, "civilStatus", 1),
                    blankToNull(form.get("street")),
                    blankToNull(form.get("barangay")),
                    blankToNull(form.get("city")),
                    blankToNull(form.get("province")),
                    blankToNull(form.get("region")),
                    blankToNull(form.get("zipCode")),
                    Date.valueOf(required(form, "birthDate")),
                    required(form, "email"),
                    required(form, "contactNumber"),

                    /*
                     * IMPORTANT:
                     * categoryId can be values like COL-004.
                     * Do not parse it as Integer.
                     */
                    required(form, "categoryId"),

                    required(form, "enrollmentStatus")
            );

            String referenceNo = nextApplicationReference();

            jdbc.update("""
                    INSERT INTO application (
                        applicant_id,
                        program_id,
                        campus_id,
                        application_status_id,
                        application_date,
                        application_semester,
                        application_academic_year,
                        application_reference_number
                    ) VALUES (?, ?, ?, ?, CURRENT_DATE, ?, ?, ?)
                    """,
                    applicantId,
                    intValue(form, "programId", 1),
                    intValue(form, "campusId", 1),
                    intValue(form, "applicationStatusId", 1),
                    required(form, "semester"),
                    required(form, "academicYear"),
                    referenceNo
            );

            ra.addFlashAttribute("success", "Applicant created with application reference " + referenceNo + ".");
            return "redirect:/applicants/" + applicantId;

        } catch (DuplicateKeyException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            ra.addFlashAttribute("error", "Email address or generated reference number already exists. Please try again.");
            return "redirect:/applicants/new";

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            ra.addFlashAttribute("error", "Create failed: " + e.getMessage());
            return "redirect:/applicants/new";
        }
    }

    @GetMapping("/applicants/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("applicant", one("""
                SELECT ap.*, e.category_name
                FROM applicant ap
                JOIN educational_background_category e
                    ON e.category_id = ap.educational_background_category_id
                WHERE ap.applicant_id = ?
                  AND COALESCE(ap.applicant_is_deleted, 0) = 0
                """, id));

        model.addAttribute("applications", jdbc.queryForList("""
                SELECT
                    a.*,
                    p.program_name,
                    p.program_code,
                    c.campus_name,
                    ast.application_status_name
                FROM application a
                JOIN program p
                    ON p.program_id = a.program_id
                JOIN campus c
                    ON c.campus_id = a.campus_id
                JOIN application_status ast
                    ON ast.application_status_id = a.application_status_id
                WHERE a.applicant_id = ?
                ORDER BY a.application_date DESC, a.application_id DESC
                """, id));

        model.addAttribute("requirements", jdbc.queryForList("""
                SELECT r.*, rt.requirement_type_name, rs.requirement_status_name
                FROM requirement r
                JOIN requirement_type rt
                    ON rt.type_id = r.requirement_type_id
                JOIN requirement_status rs
                    ON rs.status_id = r.requirement_status_id
                JOIN application a
                    ON a.application_id = r.application_id
                WHERE a.applicant_id = ?
                ORDER BY r.requirement_upload_date DESC
                """, id));

        return "applicant-detail";
    }

    @GetMapping("/applicants/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        addLookups(model);
        model.addAttribute("mode", "edit");
        model.addAttribute("applicant", one("""
                SELECT *
                FROM applicant
                WHERE applicant_id = ?
                  AND COALESCE(applicant_is_deleted, 0) = 0
                """, id));
        return "applicant-form";
    }

    @PostMapping("/applicants/{id}/edit")
    public String update(@PathVariable Integer id,
                         @RequestParam Map<String, String> form,
                         RedirectAttributes ra) {

        jdbc.update("""
                UPDATE applicant SET
                    applicant_first_name = ?,
                    applicant_middle_name = ?,
                    applicant_last_name = ?,
                    applicant_suffix = ?,
                    applicant_sex = ?,
                    applicant_civil_status = ?,
                    applicant_house_number_street = ?,
                    applicant_barangay = ?,
                    applicant_city_municipality = ?,
                    applicant_province = ?,
                    applicant_region = ?,
                    applicant_zip_code = ?,
                    applicant_birth_date = ?,
                    applicant_email_address = ?,
                    applicant_contact_number = ?,
                    educational_background_category_id = ?,
                    applicant_enrollment_status = ?,
                    applicant_updated_at = CURRENT_TIMESTAMP
                WHERE applicant_id = ?
                  AND COALESCE(applicant_is_deleted, 0) = 0
                """,
                required(form, "firstName"),
                blankToNull(form.get("middleName")),
                required(form, "lastName"),
                blankToNull(form.get("suffix")),
                intValue(form, "sex", 1),
                intValue(form, "civilStatus", 1),
                blankToNull(form.get("street")),
                blankToNull(form.get("barangay")),
                blankToNull(form.get("city")),
                blankToNull(form.get("province")),
                blankToNull(form.get("region")),
                blankToNull(form.get("zipCode")),
                Date.valueOf(required(form, "birthDate")),
                required(form, "email"),
                required(form, "contactNumber"),

                /*
                 * IMPORTANT:
                 * categoryId can be values like COL-004.
                 * Do not parse it as Integer.
                 */
                required(form, "categoryId"),

                required(form, "enrollmentStatus"),
                id
        );

        ra.addFlashAttribute("success", "Applicant updated.");
        return "redirect:/applicants/" + id;
    }

    @Transactional
    @PostMapping("/applicants/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        List<String> documentPaths = jdbc.queryForList("""
                SELECT r.requirement_image_path
                FROM requirement r
                JOIN application a
                    ON a.application_id = r.application_id
                WHERE a.applicant_id = ?
                """, String.class, id);

        int documentsDeleted = jdbc.update("""
                DELETE FROM requirement r
                USING application a
                WHERE r.application_id = a.application_id
                  AND a.applicant_id = ?
                """, id);

        int updated = jdbc.update("""
                UPDATE applicant
                SET applicant_is_deleted = 1,
                    applicant_deleted_at = CURRENT_TIMESTAMP,
                    applicant_updated_at = CURRENT_TIMESTAMP
                WHERE applicant_id = ?
                  AND COALESCE(applicant_is_deleted, 0) = 0
                """, id);

        if (updated > 0) {
            int filesDeleted = deleteUploadedFiles(documentPaths);
            ra.addFlashAttribute("success", "Applicant deleted from the active list. Deleted "
                    + documentsDeleted + " related document record(s) and "
                    + filesDeleted + " uploaded file(s).");
        } else {
            ra.addFlashAttribute("error", "Applicant was not found or was already deleted.");
        }

        return "redirect:/applicants";
    }

    private int deleteUploadedFiles(List<String> documentPaths) {
        int deleted = 0;

        for (String documentPath : documentPaths) {
            if (documentPath == null || documentPath.isBlank()) {
                continue;
            }

            try {
                Path path = Paths.get(documentPath);
                if (Files.deleteIfExists(path)) {
                    deleted++;
                }
            } catch (IOException | RuntimeException ignored) {
                // The database record is already removed. Continue deleting the other files.
            }
        }

        return deleted;
    }

    private void addLookups(Model model) {
        model.addAttribute("categories", jdbc.queryForList("""
                SELECT category_id, category_name
                FROM educational_background_category
                ORDER BY category_name
                """));

        model.addAttribute("programs", jdbc.queryForList("""
                SELECT program_id, program_name, program_code
                FROM program
                ORDER BY program_name
                """));

        model.addAttribute("campuses", jdbc.queryForList("""
                SELECT campus_id, campus_name
                FROM campus
                ORDER BY campus_name
                """));

        model.addAttribute("applicationStatuses", jdbc.queryForList("""
                SELECT application_status_id, application_status_name
                FROM application_status
                ORDER BY application_status_id
                """));

        model.addAttribute("defaultAcademicYear", Year.now().getValue() + "-" + (Year.now().getValue() + 1));
    }

    private Map<String, Object> one(String sql, Object... params) {
        return jdbc.queryForMap(sql, params);
    }

    private String nextApplicationReference() {
        int year = Year.now().getValue();
        String prefix = "APP-" + year + "-";
        String pattern = "^APP-" + year + "-[0-9]+$";

        Integer next = jdbc.queryForObject("""
                SELECT COALESCE(MAX(split_part(application_reference_number, '-', 3)::int), 0) + 1
                FROM application
                WHERE application_reference_number LIKE ?
                  AND application_reference_number ~ ?
                """,
                Integer.class,
                prefix + "%",
                pattern
        );

        if (next == null) {
            next = 1;
        }

        return prefix + String.format("%04d", next);
    }

    private String required(Map<String, String> form, String key) {
        String value = form.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
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
