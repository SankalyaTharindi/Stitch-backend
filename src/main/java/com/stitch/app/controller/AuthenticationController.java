package com.stitch.app.controller;

import com.stitch.app.dto.AuthenticationRequest;
import com.stitch.app.dto.AuthenticationResponse;
import com.stitch.app.dto.RegisterRequest;
import com.stitch.app.dto.UserDTO;
import com.stitch.app.dto.UpdateProfileRequest;
import com.stitch.app.dto.ChangePasswordRequest;
import com.stitch.app.entity.User;
import com.stitch.app.repository.UserRepository;
import com.stitch.app.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        // If email changed, ensure uniqueness
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(current.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().build();
            }
            current.setEmail(request.getEmail());
        }

        if (request.getFullName() != null) current.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) current.setPhoneNumber(request.getPhoneNumber());

        userRepository.save(current);

        return ResponseEntity.ok(UserDTO.fromUser(current));
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