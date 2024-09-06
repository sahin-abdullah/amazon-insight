package com.amazoninsight.llmreview.service;

import com.amazoninsight.llmreview.dto.RegistrationDTO;
import com.amazoninsight.llmreview.mapper.UserMapper;
import com.amazoninsight.llmreview.model.Role;
import com.amazoninsight.llmreview.model.User;
import com.amazoninsight.llmreview.repository.RoleRepository;
import com.amazoninsight.llmreview.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final JavaMailSender mailSender;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       UserMapper userMapper, RoleRepository roleRepository,
                       JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
        this.mailSender = mailSender;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public ActivationResult activateUser(String token) {
        Optional<User> userOptional = userRepository.findByActivationToken(token);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Check if the user is already activated
            if (user.isEnabled()) {
                return ActivationResult.ALREADY_ACTIVATED;
            }

            // Check if the token is expired
            if (user.getActivationTokenExpiry().isAfter(LocalDate.now())) {
                user.setEnabled(true);
                userRepository.save(user);
                return ActivationResult.ACTIVATED;
            }
        }
        return ActivationResult.INVALID_TOKEN;
    }

    public boolean resendActivationLink(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!user.isEnabled()) {
                // Generate or retrieve the activation token associated with the user
                String activationToken = UUID.randomUUID().toString();
                user.setActivationToken(activationToken);
                try {
                    userRepository.save(user);
                    sendActivationEmail(user.getEmail(), activationToken);
                    return true; // Email sent successfully
                } catch (RuntimeException e) {
                    // Log the error or handle it based on your application's requirements
                    System.err.println("Email sending failed: " + e.getMessage());
                    return false;
                }
            }
        }
        return false;
    }



    public enum ActivationResult {
        ACTIVATED,
        ALREADY_ACTIVATED,
        INVALID_TOKEN
    }

    @Transactional
    public void register(RegistrationDTO registrationDTO) {
        // Find or create the "APP_USER" role in the database
        Role defaultRole = roleRepository.findByName("APP_USER")
                .orElseGet(() -> {
                    // If not found, create and save the new role
                    Role newRole = new Role();
                    newRole.setName("APP_USER");
                    return roleRepository.save(newRole);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);

        // Map RegistrationDTO to User entity
        User user = userMapper.registrationDtoToUser(registrationDTO);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(roles);

        // Save the user to the database
        user = userRepository.save(user);

        // Send activation email
        sendActivationEmail(user.getEmail(), user.getActivationToken());
    }

    private void sendActivationEmail(String toEmail, String activationToken) {
        try {
            String htmlContent = loadAndFillTemplate(activationToken);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject("Activate Your Account");
            helper.setText(htmlContent, true); // Set to true to send HTML

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String loadAndFillTemplate(String token) throws IOException {
        ClassPathResource htmlTemplateResource = new ClassPathResource("templates/activate-email.html");
        String htmlTemplate = StreamUtils.copyToString(htmlTemplateResource.getInputStream(), StandardCharsets.UTF_8);

        // Replace placeholders
        htmlTemplate = htmlTemplate.replace("{{token}}", token);

        return htmlTemplate;
    }
}
