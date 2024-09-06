package com.amazoninsight.llmreview.controller;

import com.amazoninsight.llmreview.dto.LoginDTO;
import com.amazoninsight.llmreview.dto.RegistrationDTO;
import com.amazoninsight.llmreview.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling user-related requests.
 * Provides endpoints for user registration, activation, login, and resending activation emails.
 */
@Controller
public class UserController {

    public static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @Autowired
    public UserController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDTO", new RegistrationDTO());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registrationDTO") @Valid RegistrationDTO registrationDTO,
                               BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        if (userService.existsByUsername(registrationDTO.getUsername())) {
            bindingResult.rejectValue("username", "error.username", "Username is already taken");
            return "register";
        }

        if (userService.existsByEmail(registrationDTO.getEmail())) {
            bindingResult.rejectValue("email", "error.email", "Email is already in use");
            return "register";
        }

        try {
            userService.register(registrationDTO);
            redirectAttributes.addFlashAttribute("message", "Registration successful. Please check your email to activate your account.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginForm(Model model, @RequestParam(required = false) boolean error, HttpServletRequest request) {
        if (error) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String errorMessage = (String) session.getAttribute("errorMessage");
                if (errorMessage != null) {
                    model.addAttribute("errorMessage", errorMessage);
                    session.removeAttribute("errorMessage");  // Clear the error message
                }
            }
        }
        model.addAttribute("loginDTO", new LoginDTO());
        return "login";
    }
}
