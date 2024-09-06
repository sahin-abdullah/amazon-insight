package com.amazoninsight.llmreview.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ErrorController {

    @GetMapping("/handle-error")
    public String handleError(@RequestParam(value = "reason", required = false) String reason,
                              @RequestParam(value = "username", required = false) String username,
                              Model model) {

        if ("disabled".equals(reason)) {
            if (username != null) {
                model.addAttribute("errorCode", "403");
                model.addAttribute("errorMessage", "Account Not Activated");
                model.addAttribute("errorDetails",
                        "Your account (username: " + username +
                                ") is not activated. Please check your email for activation instructions. " +
                                "If you did not receive an activation email, you can " +
                                "<a href=\"/resend-activation?username=" + username + "\">Resend Activation Email</a>.");
            } else {
                // Handle case where username might not be available
                model.addAttribute("errorCode", "403");
                model.addAttribute("errorMessage", "Account Not Activated");
                model.addAttribute("errorDetails", "Your account is not activated. Please contact support.");
            }
        } else if ("insufficient-role".equals(reason)) {
            // Add attributes for insufficient role error
            model.addAttribute("errorCode", "403");
            model.addAttribute("errorMessage", "Access Denied");
            model.addAttribute("errorDetails", "You do not have the necessary permissions to access this page.");
        } else {
            // Add attributes for general access denial
            model.addAttribute("errorCode", "401");
            model.addAttribute("errorMessage", "Unauthorized");
            model.addAttribute("errorDetails", "You must be logged in to access this page.");
        }

        return "error"; // Return the error view
    }

    @GetMapping("/error")
    public String showErrorPage() {
        return "error";
    }

}