package com.pdts.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final JdbcTemplate jdbc;
    private final HttpServletRequest request;

    public AuditLogService(JdbcTemplate jdbc, HttpServletRequest request) {
        this.jdbc = jdbc;
        this.request = request;
    }

    public void log(String actionType, String entityType, String description) {
        log(actionType, entityType, null, description, null, null);
    }

    public void log(String actionType, String entityType, Long recordId, String description) {
        log(actionType, entityType, recordId, description, null, null);
    }

    public void log(String actionType,
                    String entityType,
                    Long recordId,
                    String description,
                    String oldValue,
                    String newValue) {

        try {
            Integer userId = getCurrentUserId();
            String ipAddress = getClientIpAddress();

            jdbc.update("""
                    INSERT INTO user_activity_log (
                        user_activity_log_user_id,
                        user_activity_log_action_type,
                        user_activity_log_entity_type,
                        archived_record_id,
                        user_activity_log_description,
                        user_activity_log_old_value,
                        user_activity_log_new_value,
                        user_activity_log_ip_address
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    userId,
                    actionType,
                    entityType,
                    recordId,
                    description,
                    oldValue,
                    newValue,
                    ipAddress
            );

        } catch (Exception e) {
            System.out.println("[AUDIT LOG ERROR] " + e.getMessage());
        }
    }

    private Integer getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getName() == null) {
                return 1;
            }

            String username = authentication.getName();

            Integer userId = jdbc.queryForObject("""
                    SELECT user_id
                    FROM app_user
                    WHERE user_username = ?
                    """, Integer.class, username);

            return userId != null ? userId : 1;

        } catch (Exception e) {
            return 1;
        }
    }

    private String getClientIpAddress() {
        try {
            String forwardedFor = request.getHeader("X-Forwarded-For");

            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }

            String realIp = request.getHeader("X-Real-IP");

            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }

            return request.getRemoteAddr();

        } catch (Exception e) {
            return "unknown";
        }
    }
}
