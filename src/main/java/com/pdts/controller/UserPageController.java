package com.pdts.controller;

import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.UUID;

import com.pdts.service.AuditLogService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class UserPageController {

    private final JdbcTemplate jdbc;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    private final HttpClient httpClient = HttpClient.newHttpClient();

private final String supabaseUrl = System.getenv("SUPABASE_URL");
private final String supabaseServiceKey = System.getenv("SUPABASE_SERVICE_ROLE_KEY");
private final String userPhotoBucket = System.getenv()
        .getOrDefault("SUPABASE_USER_PHOTO_BUCKET", "user-photos");

    public UserPageController(
            JdbcTemplate jdbc,
            AuditLogService auditLogService,
            PasswordEncoder passwordEncoder
    ) {
        this.jdbc = jdbc;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
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

        model.addAttribute("settings", jdbc.queryForList("""
        SELECT setting_key, setting_value, setting_label, setting_type, setting_options
        FROM system_setting
        WHERE setting_is_active = 1
        ORDER BY setting_key
        """));

model.addAttribute("rejectionReasons", jdbc.queryForList("""
        SELECT rejection_reason_id, rejection_reason_name, rejection_reason_description, rejection_reason_is_active
        FROM rejection_reason
        ORDER BY rejection_reason_id
        """));

model.addAttribute("campuses", jdbc.queryForList("""
        SELECT campus_id, campus_name, campus_address, campus_is_active
        FROM campus
        ORDER BY campus_name
        """));

model.addAttribute("programs", jdbc.queryForList("""
        SELECT program_id, program_code, program_name, program_is_active
        FROM program
        ORDER BY program_code
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
public String create(@RequestParam Map<String, String> form,
                     @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
                     RedirectAttributes ra) {
        try {
            String lastName = required(form, "lastName");
            String firstName = required(form, "firstName");
            String middleName = blankToNull(form.get("middleName"));
            Integer roleId = Integer.parseInt(required(form, "roleId"));
            String email = required(form, "email").toLowerCase();
            String username = required(form, "username").toLowerCase();
            String password = required(form, "password");

            String photoUrl = null;
String photoStoragePath = null;

if (photoFile != null && !photoFile.isEmpty()) {

    String extension = getFileExtension(photoFile.getOriginalFilename());

    photoStoragePath = "users/"
            + UUID.randomUUID()
            + "."
            + extension;

    String uploadUrl = supabaseUrl
            + "/storage/v1/object/"
            + userPhotoBucket
            + "/"
            + photoStoragePath;

    HttpRequest uploadRequest = HttpRequest.newBuilder()
            .uri(URI.create(uploadUrl))
            .header("Authorization", "Bearer " + supabaseServiceKey)
            .header("Content-Type", photoFile.getContentType())
            .POST(HttpRequest.BodyPublishers.ofByteArray(photoFile.getBytes()))
            .build();

    HttpResponse<String> uploadResponse = httpClient.send(
            uploadRequest,
            HttpResponse.BodyHandlers.ofString()
    );

    if (uploadResponse.statusCode() >= 300) {
        throw new RuntimeException("Photo upload failed.");
    }

    photoUrl = supabaseUrl
            + "/storage/v1/object/public/"
            + userPhotoBucket
            + "/"
            + photoStoragePath;
}

            Integer usernameCount = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM app_user
                    WHERE LOWER(user_username) = LOWER(?)
                    """, Integer.class, username);

            if (usernameCount != null && usernameCount > 0) {
                throw new IllegalArgumentException("Username already exists.");
            }

            Integer emailCount = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM app_user
                    WHERE LOWER(user_email_address) = LOWER(?)
                    """, Integer.class, email);

            if (emailCount != null && emailCount > 0) {
                throw new IllegalArgumentException("Email already exists.");
            }

           Integer userId = jdbc.queryForObject("""
        INSERT INTO app_user (
            user_last_name,
            user_first_name,
            user_middle_name,
            role_id,
            user_email_address,
            user_password_hash,
            user_username,
            user_is_active,
            user_photo_url,
            user_photo_storage_path
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
        RETURNING user_id
        """,
        Integer.class,
        lastName,
        firstName,
        middleName,
        roleId,
        email,
        passwordEncoder.encode(password),
        username,
        photoUrl,
        photoStoragePath
);

            auditLogService.log(
        "CREATE_ACCOUNT",
        "app_user",
        userId != null ? userId.longValue() : null,
        "Created staff account for " + firstName + " " + lastName,
        null,
        "Username: " + username
);

            ra.addFlashAttribute("success", "Staff account created successfully.");

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

            String username = String.valueOf(userInfo.get("user_username"));

            if ("admin".equalsIgnoreCase(username) || id == 1) {
                throw new IllegalArgumentException("Master admin account cannot be deactivated.");
            }

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

            ra.addFlashAttribute("success", "Staff account status updated successfully.");

        } catch (EmptyResultDataAccessException e) {
            ra.addFlashAttribute("error", "Staff account not found.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update staff account status: " + e.getMessage());
        }

        return "redirect:/users";
    }
@PostMapping("/profile/upload-photo")
public String uploadOwnPhoto(@RequestParam("photo") MultipartFile photo,
                             java.security.Principal principal,
                             RedirectAttributes ra) {
    try {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("Please choose a photo.");
        }

        String username = principal.getName();
        String extension = getFileExtension(photo.getOriginalFilename());

        String photoStoragePath = "users/" + username + "-" + UUID.randomUUID() + "." + extension;

        String uploadUrl = supabaseUrl
                + "/storage/v1/object/"
                + userPhotoBucket
                + "/"
                + photoStoragePath;

        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + supabaseServiceKey)
                .header("Content-Type", photo.getContentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(photo.getBytes()))
                .build();

        HttpResponse<String> uploadResponse = httpClient.send(
                uploadRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        if (uploadResponse.statusCode() >= 300) {
            throw new RuntimeException("Photo upload failed.");
        }

        String photoUrl = supabaseUrl
                + "/storage/v1/object/public/"
                + userPhotoBucket
                + "/"
                + photoStoragePath;

        jdbc.update("""
                UPDATE app_user
                SET user_photo_url = ?,
                    user_photo_storage_path = ?,
                    user_updated_at = CURRENT_TIMESTAMP
                WHERE LOWER(user_username) = LOWER(?)
                """,
                photoUrl,
                photoStoragePath,
                username
        );

        ra.addFlashAttribute("success", "Profile photo updated. Please sign out and sign in again.");

    } catch (Exception e) {
        ra.addFlashAttribute("error", "Photo upload failed: " + e.getMessage());
    }

    return "redirect:/users";
}
    
    private String required(Map<String, String> form, String key) {
        String value = form.get(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required.");
        }

        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String getFileExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
        return "jpg";
    }

    return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
}
}
