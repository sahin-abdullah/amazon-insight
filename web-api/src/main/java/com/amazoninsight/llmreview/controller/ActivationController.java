package com.amazoninsight.llmreview.controller;

import com.amazoninsight.llmreview.dto.ErrorDTO;
import com.amazoninsight.llmreview.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ActivationController {

    private final UserService userService;

    @Autowired
    public ActivationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/activate")
    public String activateAccount(@RequestParam("token") String token, RedirectAttributes redirectAttributes, Model model) {
        UserService.ActivationResult result = userService.activateUser(token);

        return switch (result) {
            case ACTIVATED -> {
                redirectAttributes.addFlashAttribute("message", "Account activated successfully!");
                yield "redirect:/login";
            }
            case ALREADY_ACTIVATED -> {
                ErrorDTO errorDTO = new ErrorDTO("403", "Account Already Activated", "This account has already been activated. Please log in.");
                redirectAttributes.addFlashAttribute("errorCode", errorDTO.getErrorCode());
                redirectAttributes.addFlashAttribute("errorMessage", errorDTO.getErrorCode());
                redirectAttributes.addFlashAttribute("errorDetails", errorDTO.getErrorDetails());
                yield "redirect:/error";
            }
            default -> {
                ErrorDTO errorDTO = new ErrorDTO("403", "Invalid Activation Token", "The activation token is invalid or has expired.");
                redirectAttributes.addFlashAttribute("errorCode", errorDTO.getErrorCode());
                redirectAttributes.addFlashAttribute("errorMessage", errorDTO.getErrorCode());
                redirectAttributes.addFlashAttribute("errorDetails", errorDTO.getErrorDetails());
                yield "redirect:/error";
            }
        };
    }

    @GetMapping("/resend-activation")
    public String resendActivation(@RequestParam("username") String username, RedirectAttributes redirectAttributes) {
        boolean result = userService.resendActivationLink(username);

        if (result) {
            redirectAttributes.addFlashAttribute("message", "Activation link resent successfully to " + username);
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to resend activation link to " + username);
            return "redirect:/login";
        }
    }
}