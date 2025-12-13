package com.stitch.app.controller;

import com.stitch.app.dto.AuthenticationRequest;
import com.stitch.app.dto.AuthenticationResponse;
import com.stitch.app.dto.RegisterRequest;
import com.stitch.app.dto.UserDTO;
import com.stitch.app.dto.UpdateProfileRequest;
import com.stitch.app.dto.ChangePasswordRequest;
import com.stitch.app.entity.Notification;
import com.stitch.app.entity.User;
import com.stitch.app.repository.UserRepository;
import com.stitch.app.service.AuthenticationService;
import com.stitch.app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    // Debug endpoint to return current authenticated user
    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(UserDTO.fromUser(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateProfile(@AuthenticationPrincipal User user,
                                                 @RequestBody UpdateProfileRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        User current = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean profileChanged = false;
        StringBuilder changes = new StringBuilder();

        // If email changed, ensure uniqueness
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(current.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().build();
            }
            current.setEmail(request.getEmail());
            profileChanged = true;
            changes.append("Email updated to ").append(request.getEmail()).append(". ");
        }

        if (request.getFullName() != null && !request.getFullName().equals(current.getFullName())) {
            current.setFullName(request.getFullName());
            profileChanged = true;
            changes.append("Name updated to ").append(request.getFullName()).append(". ");
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(current.getPhoneNumber())) {
            current.setPhoneNumber(request.getPhoneNumber());
            profileChanged = true;
            changes.append("Phone number updated. ");
        }

        current = userRepository.save(current);

        // Notify admins if customer updated their profile (and if user is a customer)
        if (profileChanged && current.getRole() == User.Role.CUSTOMER) {
            notifyAdminsAboutProfileUpdate(current, changes.toString());
        }

        return ResponseEntity.ok(UserDTO.fromUser(current));
    }

    private void notifyAdminsAboutProfileUpdate(User customer, String changes) {
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);

        for (User admin : admins) {
            notificationService.createNotification(
                    admin,
                    "Customer Profile Updated",
                    customer.getFullName() + " has updated their profile. " + changes,
                    Notification.NotificationType.PROFILE_UPDATED
            );
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User user,
                                            @RequestBody ChangePasswordRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        User current = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), current.getPassword())) {
            return ResponseEntity.badRequest().body("Current password is incorrect");
        }

        // Update password
        current.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(current);

        return ResponseEntity.ok("Password changed successfully");
    }
}