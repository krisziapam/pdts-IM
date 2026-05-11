package com.pdts.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogsPageController {

    private final JdbcTemplate jdbc;

    public LogsPageController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/logs")
    public String logs(Model model) {
        model.addAttribute("logs", jdbc.queryForList("""
                SELECT 
                    l.user_activity_log_id,
                    l.archived_record_id,
                    u.user_username,
                    l.user_activity_log_action_type,
                    l.user_activity_log_entity_type,
                    l.user_activity_log_description,
                    l.user_activity_log_old_value,
                    l.user_activity_log_new_value,
                    l.user_activity_log_ip_address,
                    l.user_activity_log_performed_at
                FROM user_activity_log l
                JOIN app_user u 
                    ON u.user_id = l.user_activity_log_user_id
                ORDER BY l.user_activity_log_performed_at DESC
                LIMIT 100
                """));

        return "logs";
    }
}
