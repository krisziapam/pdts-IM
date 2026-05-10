package com.pdts.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class UserPageController {

    private final JdbcTemplate jdbc;

    public UserPageController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/users")
    public String list(Model model) {
        model.addAttribute("users", jdbc.queryForList("""
                SELECT u.user_id, u.user_username, u.user_first_name, u.user_last_name,
                       u.user_email_address, u.user_is_active, r.role_name
                FROM app_user u
                JOIN role r ON r.role_id = u.role_id
                ORDER BY u.user_id
                """));
        return "users";
    }

    @GetMapping("/users/new")
    public String newForm(Model model) {
        model.addAttribute("roles", jdbc.queryForList("SELECT role_id, role_name FROM role ORDER BY role_id"));
        return "user-form";
    }

    @PostMapping("/users")
    public String create(@RequestParam Map<String, String> form, RedirectAttributes ra) {
        jdbc.update("""
                INSERT INTO app_user (user_last_name, user_first_name, user_middle_name, role_id,
                                      user_email_address, user_password_hash, user_username, user_is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """,
                required(form, "lastName"), required(form, "firstName"), blankToNull(form.get("middleName")),
                Integer.parseInt(required(form, "roleId")), required(form, "email"),
                "{noop}" + required(form, "password"), required(form, "username"));
        ra.addFlashAttribute("success", "Staff account created.");
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Integer id, RedirectAttributes ra) {
        jdbc.update("UPDATE app_user SET user_is_active = CASE WHEN user_is_active = 1 THEN 0 ELSE 1 END WHERE user_id = ?", id);
        ra.addFlashAttribute("success", "Staff account status updated.");
        return "redirect:/users";
    }

    private String required(Map<String, String> form, String key) {
        String value = form.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required");
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
