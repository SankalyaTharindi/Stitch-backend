package com.stitch.app.service;

import com.stitch.app.dto.AuthenticationRequest;
import com.stitch.app.dto.AuthenticationResponse;
import com.stitch.app.dto.RegisterRequest;
import com.stitch.app.dto.UserDTO;
import com.stitch.app.entity.Notification;
import com.stitch.app.entity.User;
import com.stitch.app.repository.UserRepository;
import com.stitch.app.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final NotificationService notificationService;

    public AuthenticationResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Notify all admins about new customer registration
        notifyAdminsAboutNewCustomer(user);

        // Check for customer milestone
        checkAndNotifyCustomerMilestone();

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .user(UserDTO.fromUser(user))
                .build();
    }

    private void notifyAdminsAboutNewCustomer(User customer) {
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);

        for (User admin : admins) {
            notificationService.createNotification(
                    admin,
                    "New Customer Registered",
                    "New customer " + customer.getFullName() + " has registered with email: " + customer.getEmail(),
                    Notification.NotificationType.CUSTOMER_REGISTERED
            );
        }
    }

    private void checkAndNotifyCustomerMilestone() {
        long customerCount = userRepository.findByRole(User.Role.CUSTOMER).size();

        // Check if it's a milestone (100, 200, 300, etc.)
        if (customerCount % 100 == 0) {
            List<User> admins = userRepository.findByRole(User.Role.ADMIN);

            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        "Customer Milestone Reached!",
                        "Congratulations! You have reached " + customerCount + " customers!",
                        Notification.NotificationType.CUSTOMER_MILESTONE
                );
            }
        }
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .user(UserDTO.fromUser(user))
                .build();
    }
}