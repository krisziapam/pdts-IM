// Path: src/main/java/com/pdts/controller/AuthController.java
package com.pdts.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("loginError", "Invalid credentials. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been signed out.");
        }
        return "login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }
}
