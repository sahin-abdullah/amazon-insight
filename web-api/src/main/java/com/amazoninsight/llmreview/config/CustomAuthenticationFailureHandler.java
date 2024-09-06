package com.amazoninsight.llmreview.config;

import com.amazoninsight.llmreview.exception.UserDisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String errorMessage = "Invalid username or password";

        if (exception.getCause() instanceof UserDisabledException) {
            errorMessage = "Your account is not activated. Please activate your account.";
        } else if (exception.getMessage().contains("User account has expired")) {
            errorMessage = "Your account has expired.";
        } else if (exception.getMessage().contains("credentials expired")) {
            errorMessage = "Your password has expired and needs to be changed.";
        }

        // Set error message in session temporarily
        request.getSession().setAttribute("errorMessage", errorMessage);

        // Redirect back to the login page
        super.setDefaultFailureUrl("/login?error=true");
        super.onAuthenticationFailure(request, response, exception);
    }
}
