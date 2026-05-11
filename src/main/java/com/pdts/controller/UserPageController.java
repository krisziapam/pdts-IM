package com.pdts.controller;

import com.pdts.service.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class UserPageController {

    private final JdbcTemplate jdbc;
    private final AuditLogService auditLogService;

    public UserPageController(JdbcTemplate jdbc, AuditLogService auditLogService) {
        this.jdbc = jdbc;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/users")
    public String list(Model model) {
        model.addAttribute("users", jdbc.queryForList("""
                SELECT 
                    u.user_id,
                    u.user_username,
                    u.user_first_name,
                    u.user_last_name,
                    u.user_email_address,
                    u.user_is_active,
                    r.role_name
                FROM app_user u
                JOIN role r 
                    ON r.role_id = u.role_id
                ORDER BY u.user_id
                """));

        return "users";
    }

    @GetMapping("/users/new")
    public String newForm(Model model) {
        model.addAttribute("roles", jdbc.queryForList("""
                SELECT role_id, role_name
                FROM role
                ORDER BY role_id
                """));

        return "user-form";
    }

    @PostMapping("/users")
    public String create(@RequestParam Map<String, String> form, RedirectAttributes ra) {
        try {
            Integer userId = jdbc.queryForObject("""
                    INSERT INTO app_user (
                        user_last_name,
                        user_first_name,
                        user_middle_name,
                        role_id,
                        user_email_address,
                        user_password_hash,
                        user_username,
                        user_is_active
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                    RETURNING user_id
                    """,
                    Integer.class,
                    required(form, "lastName"),
                    required(form, "firstName"),
                    blankToNull(form.get("middleName")),
                    Integer.parseInt(required(form, "roleId")),
                    required(form, "email"),
                    "{noop}" + required(form, "password"),
                    required(form, "username")
            );

            auditLogService.log(
                    "CREATE_ACCOUNT",
                    "app_user",
                    userId != null ? userId.longValue() : null,
                    "Created staff account for " + required(form, "firstName") + " " + required(form, "lastName"),
                    null,
                    "Username: " + required(form, "username")
            );

            ra.addFlashAttribute("success", "Staff account created.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create staff account: " + e.getMessage());
        }

        return "redirect:/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            Map<String, Object> userInfo = jdbc.queryForMap("""
                    SELECT 
                        user_id,
                        user_username,
                        user_first_name,
                        user_last_name,
                        user_is_active
                    FROM app_user
                    WHERE user_id = ?
                    """, id);

            Integer oldActiveStatus = ((Number) userInfo.get("user_is_active")).intValue();

            jdbc.update("""
                    UPDATE app_user
                    SET user_is_active = CASE
                        WHEN user_is_active = 1 THEN 0
                        ELSE 1
                    END
                    WHERE user_id = ?
                    """, id);

            Integer newActiveStatus = jdbc.queryForObject("""
                    SELECT user_is_active
                    FROM app_user
                    WHERE user_id = ?
                    """, Integer.class, id);

            String fullName = userInfo.get("user_first_name") + " " + userInfo.get("user_last_name");
            String username = String.valueOf(userInfo.get("user_username"));

            String oldValue = oldActiveStatus == 1 ? "Active" : "Inactive";
            String newValue = newActiveStatus != null && newActiveStatus == 1 ? "Active" : "Inactive";

            auditLogService.log(
                    "UPDATE_USER_STATUS",
                    "app_user",
                    id.longValue(),
                    "Updated staff account status for " + fullName + " (" + username + ") from " + oldValue + " to " + newValue,
                    oldValue,
                    newValue
            );

            ra.addFlashAttribute("success", "Staff account status updated.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update staff account status: " + e.getMessage());
        }

        return "redirect:/users";
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
}
